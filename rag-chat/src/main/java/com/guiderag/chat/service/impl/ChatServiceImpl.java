package com.guiderag.chat.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.guiderag.chat.client.KnowledgeServiceClient;
import com.guiderag.chat.mapper.MessageMapper;
import com.guiderag.chat.mapper.SessionMapper;
import com.guiderag.chat.model.dto.ChatReqDTO;
import com.guiderag.chat.model.dto.ChatResDTO;
import com.guiderag.chat.model.entity.Message;
import com.guiderag.chat.model.entity.Session;
import com.guiderag.chat.config.LLMConfig;
import com.guiderag.chat.service.ChatService;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;
import com.guiderag.common.exception.BizException;
import com.guiderag.common.result.Result;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.guiderag.chat.mapper.UserModelConfigMapper;
import com.guiderag.chat.model.entity.UserModelConfig;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 聊天服务实现
 * <p>
 * RAG 完整流程：
 * 1. 保存用户消息
 * 2. 调用知识库检索（OpenFeign → rag-knowledge）
 * 3. 构建 Prompt（系统提示 + 检索上下文 + 用户问题）
 * 4. 调用 LLM 生成回答（LangChain4j）
 * 5. 保存 AI 回答消息
 * 6. 返回响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final KnowledgeServiceClient knowledgeServiceClient;  // OpenFeign 客户端
    private final ChatLanguageModel chatLanguageModel;            // LangChain4j 聊天模型
    private final StreamingChatLanguageModel streamingChatModel;  // LangChain4j 流式聊天模型
    private final UserModelConfigMapper userModelConfigMapper;    // 用户模型配置

    /**
     * 默认超时秒数（用于动态创建模型时的超时配置）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /**
     * 根据数据库配置或请求中的 ModelOverride 配置，决定使用自定义模型还是默认模型（同步版本）
     */
    private ChatLanguageModel resolveModel(ChatReqDTO dto, Long userId) {
        // 优先使用数据库中用户的持久化配置
        UserModelConfig userConfig = userModelConfigMapper.selectByTenantAndUser(userId, userId);
        if (userConfig != null && userConfig.getIsActive() != null && userConfig.getIsActive() == 1) {
            log.info("使用数据库中用户自定义模型: provider={}, baseUrl={}, model={}",
                    userConfig.getProvider(), userConfig.getBaseUrl(), userConfig.getModel());
            return LLMConfig.createChatModel(
                    userConfig.getProvider(),
                    userConfig.getApiKey(),
                    userConfig.getBaseUrl(),
                    userConfig.getModel(),
                    DEFAULT_TIMEOUT_SECONDS);
        }

        // 兼容处理：如果没有数据库配置，则看前端是否传递了明确且未脱敏的配置
        ChatReqDTO.ModelOverride override = dto.getModelOverride();
        if (override != null
                && override.getBaseUrl() != null && !override.getBaseUrl().isBlank()
                && override.getModel() != null && !override.getModel().isBlank()
                && override.getApiKey() != null && !override.getApiKey().isBlank()
                && !override.getApiKey().contains("****")) {
            log.info("使用前端临时自定义模型: provider={}, baseUrl={}, model={}",
                    override.getProvider(), override.getBaseUrl(), override.getModel());
            return LLMConfig.createChatModel(
                    override.getProvider(),
                    override.getApiKey(),
                    override.getBaseUrl(),
                    override.getModel(),
                    DEFAULT_TIMEOUT_SECONDS);
        }
        return chatLanguageModel;
    }

    /**
     * 根据数据库配置或请求中的 ModelOverride 配置，决定使用自定义模型还是默认模型（流式版本）
     */
    private StreamingChatLanguageModel resolveStreamingModel(ChatReqDTO dto, Long userId) {
        // 优先使用数据库中用户的持久化配置
        UserModelConfig userConfig = userModelConfigMapper.selectByTenantAndUser(userId, userId);
        if (userConfig != null && userConfig.getIsActive() != null && userConfig.getIsActive() == 1) {
            log.info("使用数据库中用户自定义流式模型: provider={}, baseUrl={}, model={}",
                    userConfig.getProvider(), userConfig.getBaseUrl(), userConfig.getModel());
            return LLMConfig.createStreamingChatModel(
                    userConfig.getProvider(),
                    userConfig.getApiKey(),
                    userConfig.getBaseUrl(),
                    userConfig.getModel(),
                    DEFAULT_TIMEOUT_SECONDS);
        }

        // 兼容处理：如果没有数据库配置，则看前端是否传递了明确且未脱敏的配置
        ChatReqDTO.ModelOverride override = dto.getModelOverride();
        if (override != null
                && override.getBaseUrl() != null && !override.getBaseUrl().isBlank()
                && override.getModel() != null && !override.getModel().isBlank()
                && override.getApiKey() != null && !override.getApiKey().isBlank()
                && !override.getApiKey().contains("****")) {
            log.info("使用前端临时自定义流式模型: provider={}, baseUrl={}, model={}",
                    override.getProvider(), override.getBaseUrl(), override.getModel());
            return LLMConfig.createStreamingChatModel(
                    override.getProvider(),
                    override.getApiKey(),
                    override.getBaseUrl(),
                    override.getModel(),
                    DEFAULT_TIMEOUT_SECONDS);
        }
        return streamingChatModel;
    }

    /**
     * 【新增】历史消息最大条数（取最近N轮对话）
     * 防止上下文过长超出模型限制
     */
    private static final int MAX_HISTORY_MESSAGES = 10;

    /**
     * 【新增】历史消息最大Token数（防止超出模型上下文窗口）
     * 常见模型限制：GPT-3.5=4096, GPT-4=8192, DeepSeek=16384
     */
    private static final int MAX_HISTORY_TOKENS = 2000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResDTO chat(Long sessionId, ChatReqDTO dto) {
        long startTime = System.currentTimeMillis();

        // 1. 获取当前用户并校验会话
        Long userId = UserContextHolder.getUserId();
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        // 保存用户消息
        Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());
        log.info("用户消息已保存, messageId={}, sessionId={}, question={}",
                userMessage.getMessageId(), sessionId, dto.getQuestion());

        // 检索知识库
        List<ChatResDTO.CitationDTO> citations = new ArrayList<>();
        String retrievedContext = "";

        // 获取并优先使用请求中携带的 kbId
        Long queryKbId = dto.getKbId() != null ? dto.getKbId() : session.getKnowledgeBaseId();

        if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && queryKbId != null) {
            try {
                // 构建检索请求
                        RetrievalRequestDTO retrievalRequest = RetrievalRequestDTO.builder()
                                .query(dto.getQuestion())
                                .topK(dto.getTopK() != null ? dto.getTopK() : 5)
                                .minScore(0.2f)
                                .build();

                // 调用知识库服务进行向量检索
                log.info("开始向量检索，knowledgeBaseId={}, query={}, topK={}",
                        queryKbId, dto.getQuestion(), retrievalRequest.getTopK());
                Result<RetrievalResultDTO> retrievalResult = knowledgeServiceClient.
                        retrieve(queryKbId, retrievalRequest);

                // 校验检索结果
                if (retrievalResult != null && retrievalResult.isSuccess() && retrievalResult.getData() != null) {
                    RetrievalResultDTO result = retrievalResult.getData();
                    log.info("检索完成，命中 {} 个文档块，耗时 {}ms",
                            result.getChunks() != null ? result.getChunks().size() : 0,
                            result.getLatencyMs());

                    // 转换检索结果为引用信息
                    if (result.getChunks() != null && !result.getChunks().isEmpty()) {
                        citations = convertToCitations(result.getChunks());
                        retrievedContext = buildRetrievalContext(result.getChunks());
                    }
                } else {
                    log.warn("检索结果为空或失败，将不使用检索上下文进行回答");
                }
            } catch (Exception e) {
                // 检索失败不阻断对话流程，记录日志后继续
                log.error("向量检索失败，将不使用检索上下文，sessionId={}, error={}",
                        sessionId, e.getMessage(), e);
            }

        } else {
            log.info("未启用检索或会话未绑定知识库，直接调用 LLM，sessionId={}", sessionId);
        }

        // 构建系统提示词，调用LLM
        String systemPrompt = buildSystemPrompt(retrievedContext);
        String answer;
        int tokenInput = 0;
        int tokenOutput = 0;

        try {
            log.info("开始调用 LLM 生成回答，sessionId={}", sessionId);

            // 【修复】获取历史对话消息
            List<dev.langchain4j.data.message.ChatMessage> historyMessages = 
                    getHistoryMessages(sessionId, userId);

            // 【修复】构建完整消息列表（系统提示 + 历史对话 + 当前问题）
            List<dev.langchain4j.data.message.ChatMessage> allMessages = new ArrayList<>();
            allMessages.add(SystemMessage.from(systemPrompt));          // 1. 系统提示（含检索上下文）
            allMessages.addAll(historyMessages);                         // 2. 历史对话
            allMessages.add(UserMessage.from(dto.getQuestion()));       // 3. 当前问题

            log.info("[Chat] 调用LLM, 总消息数: {} (系统提示:1, 历史:{}, 当前问题:1), sessionId={}",
                    allMessages.size(), historyMessages.size(), sessionId);

            // 调用大模型（包含完整上下文）
            // 根据请求决定使用自定义模型还是默认模型
            ChatLanguageModel activeModel = resolveModel(dto, userId);
            Response<AiMessage> response = activeModel.generate(allMessages);

            // 提取回答内容
            answer = response.content().text();

            // 获取token使用情况
            if (response.tokenUsage() != null) {
                tokenInput = response.tokenUsage().inputTokenCount();
                tokenOutput = response.tokenUsage().outputTokenCount();
                log.info("LLM 调用完成，tokenInput={}, tokenOutput={}", tokenInput, tokenOutput);
            } else {
                // 若未返回token统计，则进行估算
                tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
                tokenOutput = estimateTokens(answer);
                log.info("LLM 调用完成，未返回token统计，已估算 tokenInput={}, tokenOutput={}", tokenInput, tokenOutput);
            }
        } catch (Exception e) {
            log.error("LLM 调用失败，sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new BizException("B0001", "AI 回答生成失败：" + e.getMessage());
        }

        // 保存AI回答的消息
        Message assistantMessage = saveAssistantMessage(sessionId, userId, answer, citations, tokenInput, tokenOutput);

        // 更新会话最后的消息时间
        sessionMapper.updateLastMessageAt(sessionId, userId, OffsetDateTime.now());

        // 构建响应
        long latencyMs = System.currentTimeMillis() - startTime;

        ChatResDTO response = new ChatResDTO();
        response.setMessageId(assistantMessage.getMessageId());
        response.setAnswer(answer);
        response.setCitations(citations);
        response.setTokenInput(tokenInput);
        response.setTokenOutput(tokenOutput);
        response.setLatencyMs((int) latencyMs);
        response.setGmtCreate(assistantMessage.getGmtCreate());

        log.info("Chat 完成, sessionId={}, latencyMs={}, tokenInput={}, tokenOutput={}, citationsCount={}",
                sessionId, latencyMs, tokenInput, tokenOutput, citations.size());

        return response;
    }


    /**
     * 流式聊天实现
     * 核心流程：
     * 1. 校验会话 + 保存用户消息（同步）
     * 2. 检索知识库（同步）
     * 3. 创建 SseEmitter 并立即返回（客户端建立连接）
     * 4. 异步调用流式 LLM，逐 Token 推送
     * 5. 流式完成后，保存 AI 消息到数据库
     */
    @Override
    public SseEmitter chatStream(Long sessionId, ChatReqDTO dto) {
        // 校验会话
        Long userId = UserContextHolder.getUserId();
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        // 保存用户消息
        Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());

        // 创建SSE推送器（超时时间：5分钟）
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 异步处理流式对话
        CompletableFuture.runAsync(() -> {
            try {
                // 【修复】将主线程的用户ID设置到异步线程上下文中，防止 Feign 请求丢失 x-tenant-id 和 x-user-id
                UserContextHolder.setUserId(userId);

                // 1 执行知识库检索
                List<ChatResDTO.CitationDTO> citations = new ArrayList<>();
                String retrievedContext = "";

                // 获取并优先使用请求中携带的 kbId
                Long queryKbId = dto.getKbId() != null ? dto.getKbId() : session.getKnowledgeBaseId();

                if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && queryKbId != null) {
                    try {
                        // 设置检索的参数
                        RetrievalRequestDTO retrievalRequest = RetrievalRequestDTO.builder()
                                .query(dto.getQuestion())
                                .topK(dto.getTopK() != null ? dto.getTopK() : 3)
                                .minScore(0.2f)
                                .build();

                        Result<RetrievalResultDTO> retrievalResult = knowledgeServiceClient.
                                retrieve(queryKbId, retrievalRequest);

                        if (retrievalResult.isSuccess() && retrievalResult.getData() != null) {
                            RetrievalResultDTO result = retrievalResult.getData();
                            if (result.getChunks() != null && !result.getChunks().isEmpty()) {
                                citations.addAll(convertToCitations(result.getChunks()));
                                // 将检索到的文档内容拼接成一个字符串，作为 LLM 的输入上下文
                                retrievedContext = buildRetrievalContext(result.getChunks());
                            }
                        }
                    } catch (Exception e) {
                        log.error("流式聊天 - 向量检索失败，sessionId={}, error={}", sessionId, e.getMessage(), e);
                    }
                }

                // 2 先推送引用消息
                if (!citations.isEmpty()) {
                    sendSseEvent(emitter, "citations", citations);
                }

                // 构建提示词
                String systemPrompt = buildSystemPrompt(retrievedContext);

                // 【修复】获取历史对话消息
                List<dev.langchain4j.data.message.ChatMessage> historyMessages = 
                        getHistoryMessages(sessionId, userId);

                // 【修复】构建完整消息列表
                List<dev.langchain4j.data.message.ChatMessage> allMessages = new ArrayList<>();
                allMessages.add(SystemMessage.from(systemPrompt));
                allMessages.addAll(historyMessages);
                allMessages.add(UserMessage.from(dto.getQuestion()));

                log.info("[ChatStream] 调用流式LLM, 总消息数: {} (历史:{}), sessionId={}",
                        allMessages.size(), historyMessages.size(), sessionId);

                // 调用流式 LLM
                StringBuilder fullAnswer = new StringBuilder(); // 累计完整回答
                // 使用AtomicInteger保证线程安全！
                AtomicInteger tokenCount = new AtomicInteger(); // 统计输出 token 数

                // 根据请求决定使用自定义模型还是默认模型
                StreamingChatLanguageModel activeStreamingModel = resolveStreamingModel(dto, userId);
                activeStreamingModel.generate(
                        allMessages,
                        new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {

                            // 用于逐token推送（逐字推送）
                            @Override
                            public void onNext(String token) {
                                // 每收到一个token，立即推送前端
                                fullAnswer.append(token);
                                // 统计token数量
                                tokenCount.incrementAndGet();

                                sendSseEvent(emitter, "token", token);

                            }

                            // 流式调用完成后，进行onComplete回调
                            @Override
                            public void onComplete(Response<AiMessage> response) {
                                try {
                                    // 保存AI消息到数据库
                                    String answer = fullAnswer.toString();

                                    Integer tokenInput = null;
                                    Integer tokenOutput = null;

                                    // 计算token使用量
                                    if (response.tokenUsage() != null) {
                                        tokenInput = response.tokenUsage().inputTokenCount();
                                        tokenOutput = response.tokenUsage().outputTokenCount();
                                    } else {
                                        tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
                                        tokenOutput = tokenCount.get();
                                    }

                                    // 保存消息
                                    Message aiMessage = saveAssistantMessage(
                                            sessionId,
                                            userId,
                                            answer,
                                            citations,
                                            tokenInput,
                                            tokenOutput
                                    );

                                    // 更新会话时间
                                    sessionMapper.updateLastMessageAt(sessionId, userId, OffsetDateTime.now());

                                    // 推送元数据
                                    HashMap<String, Object> metaData = new HashMap<>();
                                    metaData.put("messageId", aiMessage.getMessageId());
                                    metaData.put("tokenInput", tokenInput);
                                    metaData.put("tokenOutput", tokenOutput);
                                    metaData.put("done", true);

                                    sendSseEvent(emitter, "metaData", metaData);

                                    // 完成推送
                                    emitter.complete();

                                    log.info("流式Chat完成：sessionId={}, messageId={}, tokens={}",
                                            sessionId, aiMessage.getMessageId(), tokenCount.get());
                                } catch (Exception e) {
                                    log.error("保存流式消息失败", e);
                                    sendSseError(emitter, "保存消息失败");
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("流式LLM调用失败", error);
                                sendSseError(emitter, "AI 服务暂时不可用");
                            }
                        }
                );
            }catch (Exception e) {
                log.error("流式聊天处理失败，sessionId={}, error={}", sessionId, e.getMessage(), e);
                sendSseError(emitter, "聊天处理失败：" + e.getMessage());
            } finally {
                // 【修复】清理子线程的用户上下文，防止线程池复用导致上下文数据污染
                UserContextHolder.clear();
            }
        });
        return emitter;
    }

    // 发送SSE事件
    private void sendSseEvent(SseEmitter sseEmitter, String event, Object data) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .name(event)
                    .data(data));
        } catch (Exception e) {
            log.error("发送 SSE 事件失败，event={}, error={}", event, e.getMessage(), e);
            sseEmitter.completeWithError(e);
        }
    }

    // 发送 SSE 错误事件
    private void sendSseError(SseEmitter sseEmitter, String errorMsg) {
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", errorMsg);
            sseEmitter.send(SseEmitter.event()
                    .name("error")
                    .data(error));
            sseEmitter.completeWithError(new RuntimeException(errorMsg));
        } catch (Exception e) {
            log.error("发送 SSE 错误事件失败，errorMsg={}, error={}", errorMsg, e.getMessage(), e);
            sseEmitter.completeWithError(e);
        }
    }


    private Map<String, Object> buildRetrievalContextMap(List<ChatResDTO.CitationDTO> citations) {
        Map<String, Object> context = new HashMap<>();
        if (citations != null && !citations.isEmpty()) {
            context.put("chunkIds", citations.stream()
                    .map(ChatResDTO.CitationDTO::getChunkId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        return context;
    }

    // 保存用户消息
    private Message saveUserMessage(Long sessionId, Long userId, String content) {
        Message message = new Message();
        message.setMessageId(IdUtil.getSnowflakeNextId());
        message.setTenantId(userId);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(content);
        message.setContentType("text");
        message.setReferencedChunkIds("[]");
        message.setRetrievalContext("{}");
        message.setTokenInput(0);
        message.setTokenOutput(0);
        message.setLatencyMs(0);
        message.setIsDeleted(0);

        messageMapper.insert(message);
        return message;
    }

    // 保存AI回答的消息
    private Message saveAssistantMessage(Long sessionId, Long userId, String answer, List<ChatResDTO.CitationDTO> citations,
                                         int tokenInput, int tokenOutput) {
        // 构建引用上下文
        HashMap<String, Object> retrievalContext = new HashMap<>();
        retrievalContext.put("citations", citations);

        // 提取chunkId列表
        List<String> chunkIds = citations.stream()
                .map(ChatResDTO.CitationDTO::getChunkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Message message = new Message();
        message.setMessageId(IdUtil.getSnowflakeNextId());
        message.setTenantId(userId);
        message.setSessionId(sessionId);
        message.setUserId(null); // assistant 消息无 userId
        message.setRole("assistant");
        message.setContent(answer);
        message.setContentType("text");
        message.setReferencedChunkIds(JSON.toJSONString(chunkIds));
        message.setRetrievalContext(JSON.toJSONString(retrievalContext));
        message.setTokenInput(tokenInput);
        message.setTokenOutput(tokenOutput);
        message.setLatencyMs(0);
        message.setIsDeleted(0);

        messageMapper.insert(message);
        return message;
    }

    // 转换检索结果为引用消息
    private List<ChatResDTO.CitationDTO> convertToCitations(List<RetrievalResultDTO.ChunkResultDTO> chunks) {
        return chunks.stream().map(chunk -> {
            ChatResDTO.CitationDTO citation = new ChatResDTO.CitationDTO();
            citation.setDocId(chunk.getDocumentId() != null ? chunk.getDocumentId().toString() : null);
            citation.setDocTitle(chunk.getDocumentTitle());
            citation.setChunkId(chunk.getChunkId());
            citation.setChunkIndex(chunk.getChunkIndex());
            citation.setScore(chunk.getScore() != null ? chunk.getScore().doubleValue() : null);
            citation.setSourceText(chunk.getContentText());
            return citation;
        }).collect(Collectors.toList());
    }

    // 构建检索上下文文本
    // 将检索到的文档内容拼接成一个字符串，作为 LLM 的输入上下文
    private String buildRetrievalContext(List<RetrievalResultDTO.ChunkResultDTO> chunks) {
        if (chunks == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalResultDTO.ChunkResultDTO chunk = chunks.get(i);
            builder.append(String.format("[文档片段 %d]", i + 1));

            // 添加来源信息
            if (chunk.getDocumentTitle() != null) {
                builder.append(String.format("来源： 《%s》", chunk.getDocumentTitle()));
            }
            if (chunk.getChunkIndex() != null) {
                builder.append(String.format("（第%d段）", chunk.getChunkIndex()));
            }
            if (chunk.getScore() != null) {
                builder.append(String.format("，相关度：%.2f", chunk.getScore()));
            }
            builder.append("\n");

            // 添加内容
            builder.append(chunk.getContentText()).append("\n\n");
        }
        return builder.toString();
    }

    /**
     * 构建系统提示词
     * 包含检索到的知识库上下文
     */
    private String buildSystemPrompt(String retrievedContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的企业知识库助手。请基于以下参考资料回答用户问题。\n\n");
        sb.append("## 回答要求\n");
        sb.append("1. **优先使用参考资料**：如果参考资料中有相关信息，请直接引用\n");
        sb.append("2. **保持准确性**：不要编造或臆测不确定的信息\n");
        sb.append("3. **明确知识来源**：\n");
        sb.append("   - 如果基于参考资料回答，可以说\"根据参考资料...\"\n");
        sb.append("   - 如果参考资料不足，可以使用你的知识补充，但需说明\"参考资料未覆盖此内容，根据通用知识...\"\n");
        sb.append("4. **格式规范**：使用 Markdown 格式组织内容，便于阅读\n");
        sb.append("5. **回答风格**：专业、简洁、友好\n\n");

        if (retrievedContext != null && !retrievedContext.trim().isEmpty()) {
            sb.append("## 参考资料\n\n");
            sb.append(retrievedContext);
            sb.append("\n## 用户问题\n\n");
        } else {
            sb.append("## 说明\n\n");
            sb.append("当前没有检索到相关的知识库内容，请基于你的通用知识回答。\n\n");
            sb.append("## 用户问题\n\n");
        }

        return sb.toString();
    }

    /**
     * 估算 Token 数量（当 LLM 未返回精确统计时使用）
     * 粗略估算：中文约 1.5 token/字，英文约 0.25 token/word
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 统计中文字符
        long chineseChars = text.chars()
                .filter(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)
                .count();

        // 统计英文单词（简单按空格分割）
        long englishWords = Arrays.stream(text.split("\\s+"))
                .filter(word -> word.matches(".*[a-zA-Z].*"))
                .count();

        // 粗略估算
        return (int) (chineseChars * 1.5 + englishWords * 0.25);
    }

    /**
     * 【新增】获取会话历史消息（用于多轮对话上下文）
     * 
     * 修复说明：
     * - 原代码只发送 SystemMessage + UserMessage，缺少历史对话
     * - 导致 AI 无法理解上下文，每次都是独立问答
     * 
     * 实现策略：
     * 1. 从数据库获取最近的 N 条历史消息
     * 2. Token预算控制，防止超出模型限制
     * 3. 只包含 user 和 assistant 角色的消息
     * 4. 按时间正序排列（数据库倒序查询后需反转）
     * 
     * @param sessionId 会话ID
     * @param tenantId 租户ID
     * @return LangChain4j格式的历史消息列表
     */
    private List<dev.langchain4j.data.message.ChatMessage> getHistoryMessages(Long sessionId, Long tenantId) {
        // 从数据库获取最近的历史消息（倒序）
        List<Message> historyList = messageMapper.selectRecentBySessionId(sessionId, tenantId, MAX_HISTORY_MESSAGES);
        
        if (historyList == null || historyList.isEmpty()) {
            log.debug("[ChatHistory] 会话无历史消息, sessionId={}", sessionId);
            return Collections.emptyList();
        }

        // 反转为时间正序（从旧到新）
        Collections.reverse(historyList);

        // 转换为LangChain4j消息格式，同时进行Token预算控制
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        int totalTokens = 0;

        for (Message msg : historyList) {
            // Token预算检查
            int msgTokens = estimateTokens(msg.getContent());
            if (totalTokens + msgTokens > MAX_HISTORY_TOKENS) {
                log.debug("[ChatHistory] 达到Token上限({}), 停止添加历史消息, 已添加: {} 条",
                        MAX_HISTORY_TOKENS, messages.size());
                break;
            }
            totalTokens += msgTokens;

            // 根据角色转换消息类型
            switch (msg.getRole()) {
                case "user" -> messages.add(UserMessage.from(msg.getContent()));
                case "assistant" -> messages.add(AiMessage.from(msg.getContent()));
                case "system" -> messages.add(SystemMessage.from(msg.getContent()));
                // tool 消息暂不处理
                default -> log.debug("[ChatHistory] 跳过未知角色消息: {}", msg.getRole());
            }
        }

        log.info("[ChatHistory] 加载历史消息成功, sessionId={}, count={}, tokens≈{}",
                sessionId, messages.size(), totalTokens);
        return messages;
    }


}
