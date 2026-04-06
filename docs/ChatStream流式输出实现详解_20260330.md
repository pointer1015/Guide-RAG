# Chat 模块流式输出（SSE）实现详解

> **文档版本**: 1.0  
> **创建日期**: 2026-03-30  
> **项目名称**: Guide-RAG 企业级知识库助手  
> **模块**: rag-chat  

---

## 📑 目录

1. [概述](#1-概述)
2. [同步模式 vs 流式模式对比](#2-同步模式-vs-流式模式对比)
3. [完整实现方案](#3-完整实现方案)
   - [3.1 ChatService.java - 接口定义](#31-chatservicejava---接口定义)
   - [3.2 ChatServiceImpl.java - 流式方法实现](#32-chatserviceimpljava---流式方法实现)
   - [3.3 LLMConfig.java - 流式模型配置](#33-llmconfigjava---流式模型配置)
   - [3.4 SessionController.java - 流式端点](#34-sessioncontrollerjava---流式端点)
4. [chatStream 方法逐步详解](#4-chatstream-方法逐步详解)
5. [前端调用示例](#5-前端调用示例)
6. [重难点解析](#6-重难点解析)
7. [遇到的问题与解决方案](#7-遇到的问题与解决方案)
8. [最佳实践总结](#8-最佳实践总结)
9. [测试指南](#9-测试指南)

---

## 1. 概述

### 1.1 什么是流式输出？

流式输出（Streaming Output）是指服务端不等待完整响应生成完毕，而是将生成的内容**逐片段（Token）推送**给客户端的技术。在大语言模型（LLM）场景中，这意味着用户可以像看"打字机"一样实时看到 AI 生成的回答。

### 1.2 为什么需要流式输出？

| 对比维度 | 同步模式 | 流式模式 |
|---------|---------|---------|
| **用户体验** | 等待 5-30 秒后一次性显示 | 立即开始逐字显示 |
| **感知延迟** | 首字节延迟 = 完整生成时间 | 首字节延迟 ≈ 0.5-1 秒 |
| **心理感受** | 用户焦虑："卡住了？" | 用户安心："正在思考..." |
| **适用场景** | API 调用、后台任务 | 前端对话界面 |

### 1.3 技术选型：SSE vs WebSocket

| 特性 | SSE (Server-Sent Events) | WebSocket |
|------|--------------------------|-----------|
| **通信方向** | 单向（服务端→客户端） | 双向 |
| **协议** | HTTP/1.1 或 HTTP/2 | 独立协议（需握手） |
| **自动重连** | 浏览器原生支持 | 需手动实现 |
| **复杂度** | 低 | 高 |
| **适用场景** | 实时通知、流式输出 | 聊天、游戏、协同编辑 |

**结论**：对于 LLM 流式输出，**SSE 是更合适的选择**——单向推送、实现简单、浏览器原生支持。

---

## 2. 同步模式 vs 流式模式对比

### 2.1 时序对比图

**同步模式时序：**
```
用户发送问题
    ↓
服务端接收请求
    ↓
调用知识库检索（500ms）
    ↓
调用 LLM 生成（10-30秒）← 用户在等待...
    ↓
返回完整响应
    ↓
前端显示完整回答
```

**流式模式时序：**
```
用户发送问题
    ↓
服务端接收请求
    ↓
【立即返回 SSE 连接】← 用户看到"正在思考..."
    ↓
调用知识库检索（500ms）
    ↓
推送 citations 事件 → 前端显示引用来源
    ↓
调用 LLM 生成
    ├─ 推送 token "R" → 前端显示 "R"
    ├─ 推送 token "AG" → 前端显示 "RAG"
    ├─ 推送 token "是" → 前端显示 "RAG是"
    └─ ... 持续推送
    ↓
推送 metadata 事件（包含 messageId）
    ↓
关闭连接
```

### 2.2 代码对比

**同步方法签名：**
```java
ChatResDTO chat(Long sessionId, ChatReqDTO dto);
// 返回值：包含完整回答的 DTO 对象
// 阻塞：等待 LLM 生成完毕才返回
```

**流式方法签名：**
```java
SseEmitter chatStream(Long sessionId, ChatReqDTO dto);
// 返回值：SSE 推送器（HTTP 长连接）
// 非阻塞：立即返回，异步推送数据
```

---

## 3. 完整实现方案

### 3.1 ChatService.java - 接口定义

```java
package com.guiderag.chat.service;

import com.guiderag.chat.model.dto.ChatReqDTO;
import com.guiderag.chat.model.dto.ChatResDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    /**
     * 同步聊天接口
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return 聊天响应（包含回答和引用）
     */
    ChatResDTO chat(Long sessionId, ChatReqDTO dto);

    /**
     * 流式聊天接口（SSE）
     * 
     * 通过 Server-Sent Events 实时推送 LLM 生成的每个 Token，
     * 实现类似 ChatGPT 的打字机效果。
     *
     * 事件类型：
     * - citations: 引用信息（知识库检索结果）
     * - token: 每个生成的 Token
     * - metaData: 元数据（messageId、tokenUsage、done 标志）
     * - error: 错误信息
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return SseEmitter 用于推送流式数据
     */
    SseEmitter chatStream(Long sessionId, ChatReqDTO dto);
}
```

**设计要点：**
- 同步和流式两个方法并存，满足不同场景需求
- 流式方法返回 `SseEmitter`，而非 `ChatResDTO`
- 文档注释说明事件类型，便于前端对接

---

### 3.2 ChatServiceImpl.java - 流式方法实现

```java
/**
 * 流式聊天实现
 * 
 * 核心流程：
 * 1. 校验会话 + 保存用户消息（同步）
 * 2. 创建 SseEmitter 并立即返回（客户端建立连接）
 * 3. 异步处理：检索知识库 → 构建 Prompt → 调用流式 LLM
 * 4. 逐 Token 推送给前端
 * 5. 流式完成后，保存 AI 消息到数据库
 */
@Override
public SseEmitter chatStream(Long sessionId, ChatReqDTO dto) {
    // ========== 第1步：校验会话（同步执行）==========
    Long userId = UserContextHolder.getUserId();
    Session session = sessionMapper.selectById(sessionId, userId);
    if (session == null) {
        throw new BizException("会话不存在或无权访问");
    }

    // ========== 第2步：保存用户消息（同步执行）==========
    Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());

    // ========== 第3步：创建 SSE 推送器 ==========
    // 超时时间设置为 5 分钟（LLM 生成时间 + 网络延迟 + 安全冗余）
    SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

    // ========== 第4步：异步处理核心逻辑 ==========
    CompletableFuture.runAsync(() -> {
        try {
            // ---------- 4.1 知识库检索 ----------
            List<ChatResDTO.CitationDTO> citations = new ArrayList<>();
            String retrievedContext = null;

            if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && session.getKnowledgeBaseId() != null) {
                try {
                    RetrievalRequest retrievalRequest = new RetrievalRequest();
                    retrievalRequest.setQuery(dto.getQuestion());
                    retrievalRequest.setTopK(dto.getTopK() != null ? dto.getTopK() : 3);
                    retrievalRequest.setMinScore(0.5);

                    Result<RetrievalResult> retrievalResult = knowledgeServiceClient
                            .retrieve(session.getKnowledgeBaseId(), retrievalRequest);

                    if (retrievalResult.isSuccess() && retrievalResult.getData() != null) {
                        RetrievalResult result = retrievalResult.getData();
                        if (result.getChunks() != null && !result.getChunks().isEmpty()) {
                            citations.addAll(convertToCitations(result.getChunks()));
                            retrievedContext = buildRetrievalContext(result.getChunks());
                        }
                    }
                } catch (Exception e) {
                    log.error("流式聊天 - 向量检索失败，降级为纯LLM对话", e);
                    // 检索失败不抛异常，降级为纯 LLM 模式
                }
            }

            // ---------- 4.2 先推送引用信息 ----------
            if (!citations.isEmpty()) {
                sendSseEvent(emitter, "citations", citations);
            }

            // ---------- 4.3 构建 Prompt ----------
            String systemPrompt = buildSystemPrompt(retrievedContext);

            // ---------- 4.4 调用流式 LLM ----------
            StringBuilder fullAnswer = new StringBuilder();
            AtomicInteger tokenCount = new AtomicInteger(0);

            // 创建最终引用列表的副本（Lambda 中需要 effectively final）
            final List<ChatResDTO.CitationDTO> finalCitations = citations;

            streamingChatModel.generate(
                    List.of(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from(dto.getQuestion())
                    ),
                    new StreamingResponseHandler<AiMessage>() {

                        @Override
                        public void onNext(String token) {
                            // 每收到一个 Token，立即推送给前端
                            fullAnswer.append(token);
                            tokenCount.incrementAndGet();
                            sendSseEvent(emitter, "token", token);
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            try {
                                // 5. 流式完成，保存 AI 消息
                                String answer = fullAnswer.toString();

                                Integer tokenInput = null;
                                Integer tokenOutput = null;

                                if (response.tokenUsage() != null) {
                                    tokenInput = response.tokenUsage().inputTokenCount();
                                    tokenOutput = response.tokenUsage().outputTokenCount();
                                } else {
                                    tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
                                    tokenOutput = tokenCount.get();
                                }

                                // 保存消息到数据库
                                Message aiMessage = saveAssistantMessage(
                                        sessionId,
                                        userId,
                                        answer,
                                        finalCitations,
                                        tokenInput,
                                        tokenOutput
                                );

                                // 更新会话最后消息时间
                                sessionMapper.updateLastMessageAt(sessionId, userId, LocalDateTime.now());

                                // 6. 推送元数据
                                HashMap<String, Object> metaData = new HashMap<>();
                                metaData.put("messageId", aiMessage.getMessageId());
                                metaData.put("tokenInput", tokenInput);
                                metaData.put("tokenOutput", tokenOutput);
                                metaData.put("done", true);

                                sendSseEvent(emitter, "metaData", metaData);

                                // 7. 完成推送，关闭连接
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

        } catch (Exception e) {
            log.error("流式对话异常", e);
            sendSseError(emitter, "服务异常");
        }
    });

    // 立即返回 SseEmitter（不等待异步处理完成）
    return emitter;
}

// ==================== 辅助方法 ====================

/**
 * 发送 SSE 事件
 */
private void sendSseEvent(SseEmitter sseEmitter, String event, Object data) {
    try {
        sseEmitter.send(SseEmitter.event()
                .name(event)
                .data(data));
    } catch (Exception e) {
        log.error("发送 SSE 事件失败，event={}", event, e);
        sseEmitter.completeWithError(e);
    }
}

/**
 * 发送 SSE 错误事件
 */
private void sendSseError(SseEmitter sseEmitter, String errorMsg) {
    try {
        Map<String, String> error = new HashMap<>();
        error.put("error", errorMsg);
        sseEmitter.send(SseEmitter.event()
                .name("error")
                .data(error));
        sseEmitter.completeWithError(new RuntimeException(errorMsg));
    } catch (Exception e) {
        log.error("发送 SSE 错误事件失败", e);
        sseEmitter.completeWithError(e);
    }
}
```

---

### 3.3 LLMConfig.java - 流式模型配置

```java
package com.guiderag.chat.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 模型配置
 * 
 * 提供两个 Bean：
 * - ChatLanguageModel：同步模型（等待完整响应）
 * - StreamingChatLanguageModel：流式模型（逐 Token 回调）
 */
@Slf4j
@Configuration
public class LLMConfig {

    @Value("${llm.provider:deepseek}")
    private String provider;

    @Value("${llm.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${llm.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${llm.openai.model:gpt-4o}")
    private String openaiModel;

    @Value("${llm.timeout:60}")
    private Integer timeoutSeconds;

    // ==================== 同步模型 Bean ====================
    
    /**
     * 同步聊天模型
     * 适用于：API 调用、后台任务
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化同步LLM模型: provider={}, timeout={}s", provider, timeoutSeconds);

        if ("openai".equalsIgnoreCase(provider)) {
            return buildOpenAiModel(openaiApiKey, openaiBaseUrl, openaiModel);
        } else {
            return buildOpenAiModel(deepseekApiKey, deepseekBaseUrl, deepseekModel);
        }
    }

    // ==================== 流式模型 Bean ====================
    
    /**
     * 流式聊天模型
     * 适用于：前端对话界面（打字机效果）
     * 
     * 与同步模型的关键区别：
     * - ChatLanguageModel.generate() 阻塞等待完整响应
     * - StreamingChatLanguageModel.generate() 通过回调逐 Token 推送
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化流式LLM模型: provider={}, timeout={}s", provider, timeoutSeconds);

        if ("openai".equalsIgnoreCase(provider)) {
            return buildStreamingOpenAiModel(openaiApiKey, openaiBaseUrl, openaiModel);
        } else {
            return buildStreamingOpenAiModel(deepseekApiKey, deepseekBaseUrl, deepseekModel);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建同步 OpenAI 兼容模型
     */
    private ChatLanguageModel buildOpenAiModel(String apiKey, String baseUrl, String model) {
        log.info("构建同步模型：baseUrl={}, model={}", baseUrl, model);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.5)                          // 温度参数
                .timeout(Duration.ofSeconds(timeoutSeconds)) // 超时时间
                .maxRetries(2)                              // 重试次数
                .logRequests(true)                          // 记录请求日志
                .logResponses(true)                         // 记录响应日志
                .build();
    }

    /**
     * 构建流式 OpenAI 兼容模型
     * 
     * 注意事项：
     * 1. 流式模型不支持 maxRetries（流式重试无意义）
     * 2. logRequests/logResponses 会输出每个 Token（调试时有用，生产建议关闭）
     */
    private StreamingChatLanguageModel buildStreamingOpenAiModel(String apiKey, String baseUrl, String model) {
        log.info("构建流式模型：baseUrl={}, model={}", baseUrl, model);

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.5)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)   // 生产环境关闭（避免日志量爆炸）
                .logResponses(false)
                .build();
    }
}
```

**核心要点：**

| 模型类型 | 类名 | 调用方式 | 用途 |
|---------|------|---------|------|
| 同步模型 | `ChatLanguageModel` | `generate()` 阻塞 | API、后台任务 |
| 流式模型 | `StreamingChatLanguageModel` | `generate()` + 回调 | 前端打字机效果 |

---

### 3.4 SessionController.java - 流式端点

```java
/**
 * 发送消息（流式版本 - SSE）
 * 
 * 适用场景：
 * - Web 前端对话界面（打字机效果）
 * - 需要实时反馈的场景
 * 
 * 事件类型：
 * - citations: 引用信息（如果启用检索）
 * - token: 每个生成的 Token
 * - metaData: 元数据（包含 messageId、tokenUsage、done 标志）
 * - error: 错误信息
 */
@Operation(
        summary = "发送消息（流式SSE）",
        description = "向指定会话发送消息，通过 SSE 实时推送 AI 回答的每个 Token（打字机效果）。"
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "SSE 连接建立成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
})
@PostMapping(value = "/{sessionId}/chat-stream", produces = "text/event-stream")
public SseEmitter chatStream(
        @Parameter(description = "会话ID", required = true)
        @PathVariable Long sessionId,
        @Valid @RequestBody ChatReqDTO dto) {

    return chatService.chatStream(sessionId, dto);
}
```

**关键注解：**
- `produces = "text/event-stream"`：声明响应类型为 SSE
- 返回 `SseEmitter` 而非 `Result<T>`：Spring 会自动处理 SSE 响应

---

## 4. chatStream 方法逐步详解

### 4.1 第1步：校验会话（同步执行）

```java
Long userId = UserContextHolder.getUserId();
Session session = sessionMapper.selectById(sessionId, userId);
if (session == null) {
    throw new BizException("会话不存在或无权访问");
}
```

**为什么在异步前做校验？**

| 阶段 | 校验失败后果 | 客户端表现 |
|------|-------------|-----------|
| **异步前校验（正确）** | 直接抛异常 | 收到标准 4xx 错误响应 |
| **异步后校验（错误）** | 异常无法被 Spring 捕获 | 收到 200 OK 但无数据 |

```java
// ❌ 错误示例：异步后校验
@Override
public SseEmitter chatStream(Long sessionId, ChatReqDTO dto) {
    SseEmitter emitter = new SseEmitter();
    
    CompletableFuture.runAsync(() -> {
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("会话不存在");  // ← Spring 捕获不到！
        }
    });
    
    return emitter;  // ← 已经返回 200 OK 了
}
```

---

### 4.2 第2步：保存用户消息（同步执行）

```java
Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());
```

**为什么必须在异步前保存？**

| 保存时机 | LLM 调用失败时 | 用户消息状态 |
|---------|---------------|-------------|
| **异步前保存（正确）** | AI 消息不保存 | ✅ 用户消息已保存 |
| **异步后保存（错误）** | 整个事务回滚 | ❌ 用户消息丢失 |

**业务意义：**
- 用户消息是用户的输入，必须可靠保存（审计、历史记录）
- 即使 LLM 失败，用户也能看到自己发的消息

---

### 4.3 第3步：创建 SSE 推送器

```java
SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
```

**超时时间设置考虑：**

| 场景 | 典型耗时 | 建议超时 |
|------|---------|---------|
| LLM 生成短回答 | 5-15 秒 | - |
| LLM 生成长回答 | 30-60 秒 | - |
| 极端情况（队列等待） | 1-2 分钟 | - |
| **推荐设置** | - | **5 分钟** |

**超时后会发生什么？**
```java
emitter.onTimeout(() -> {
    log.warn("SSE连接超时");
    emitter.complete();  // 主动关闭连接
});
```

---

### 4.4 第4步：异步处理核心逻辑

```java
CompletableFuture.runAsync(() -> {
    // 核心逻辑...
});
```

**为什么必须异步？**

**同步处理的问题：**
```java
// ❌ 同步处理（阻塞 Tomcat 线程）
@PostMapping("/chat-stream")
public SseEmitter chatStream(...) {
    SseEmitter emitter = new SseEmitter();
    
    // 这里耗时 10 秒，Tomcat 线程被阻塞！
    streamingChatModel.generate(...);
    
    return emitter;
}
```

| 指标 | Tomcat 默认配置 | 问题 |
|------|----------------|------|
| 最大线程数 | 200 | 如果 200 个请求都卡在 LLM 调用 |
| 每次调用耗时 | 10 秒 | 10 秒内无法接受新请求 |
| 最大 QPS | 200 / 10 = 20 | 吞吐量极低 |

**异步处理的优势：**
```java
// ✅ 异步处理（立即释放 Tomcat 线程）
@PostMapping("/chat-stream")
public SseEmitter chatStream(...) {
    SseEmitter emitter = new SseEmitter();
    
    CompletableFuture.runAsync(() -> {
        // 在独立线程中执行（不占用 Tomcat 线程）
        streamingChatModel.generate(...);
    });
    
    return emitter;  // ← Tomcat 线程立即返回
}
```

---

### 4.5 第4.1步：知识库检索

```java
if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && session.getKnowledgeBaseId() != null) {
    try {
        // 调用检索...
    } catch (Exception e) {
        log.error("检索失败，降级为纯LLM对话", e);
        // 不抛异常！
    }
}
```

**为什么检索失败只打日志不抛异常？**

**降级策略（Graceful Degradation）：**

| 场景 | 检索状态 | 处理方式 |
|------|---------|---------|
| 知识库可用 | ✅ 返回文档 | RAG 模式：LLM + 检索上下文 |
| 知识库不可用 | ❌ 超时/异常 | **降级为纯 LLM 模式** |

**代码流程：**
```java
String retrievedContext = null;  // 初始化为 null

if (检索启用 && 有知识库) {
    try {
        retrievedContext = 调用检索();
    } catch (Exception e) {
        // 检索失败，retrievedContext 保持 null
    }
}

// 构建 Prompt（自动适配）
String systemPrompt = buildSystemPrompt(retrievedContext);
// 如果 retrievedContext == null → 返回纯 LLM 的 Prompt
// 如果 retrievedContext != null → 返回 RAG 的 Prompt

// LLM 生成（无论检索成功与否都能执行）
streamingChatModel.generate(systemPrompt, ...);
```

**为什么用 `Boolean.TRUE.equals()`？**

```java
// ❌ 危险写法（NPE 风险）
if (dto.getEnableRetrieval() == true) { ... }

// ❌ 危险写法（NPE 风险）
if (dto.getEnableRetrieval()) { ... }

// ✅ 安全写法（null-safe）
if (Boolean.TRUE.equals(dto.getEnableRetrieval())) { ... }
```

| dto.getEnableRetrieval() | `== true` | `Boolean.TRUE.equals()` |
|-------------------------|-----------|------------------------|
| `true` | ✅ true | ✅ true |
| `false` | ❌ false | ❌ false |
| `null` | ❌ false | ❌ false（安全） |

---

### 4.6 第4.2步：先推送引用信息

```java
if (!citations.isEmpty()) {
    sendSseEvent(emitter, "citations", citations);
}
```

**为什么先推送引用，再推送内容？**

**前端渲染时序：**
```
用户问："什么是RAG？"
    ↓
1. SSE 连接建立
    ↓
2. 推送 citations → 前端显示"正在查询以下文档..."
    ↓
3. 推送 token → 前端逐字显示回答
    ↓
4. 推送 metaData → 前端显示"回答完成"
```

**如果顺序反了：**
- 用户先看到 AI 回答逐字显示
- 回答完了才看到"来源"
- 用户体验差（不知道答案从哪来）

---

### 4.7 第4.4步：调用流式 LLM

```java
StringBuilder fullAnswer = new StringBuilder();  // 累积完整回答
AtomicInteger tokenCount = new AtomicInteger(0); // 统计 Token 数

streamingChatModel.generate(
        List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(dto.getQuestion())
        ),
        new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullAnswer.append(token);
                tokenCount.incrementAndGet();
                sendSseEvent(emitter, "token", token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                // 保存消息、推送元数据、关闭连接
            }

            @Override
            public void onError(Throwable error) {
                sendSseError(emitter, "AI 服务暂时不可用");
            }
        }
);
```

**Q1: 为什么需要 `StringBuilder fullAnswer`？**
- `onNext()` 每次只收到一个 Token（如 "R"、"AG"、"是"）
- 数据库需要保存完整回答（"RAG是检索增强生成..."）
- 必须在内存中累积所有 Token

**Q2: 为什么需要 `AtomicInteger tokenCount`？**
- LLM 提供商可能不返回 `tokenUsage`
- 需要手动计数作为降级方案

**Q3: 为什么用 `AtomicInteger` 而不是 `int`？**
- `StreamingResponseHandler` 的回调可能在不同线程执行
- `AtomicInteger` 保证原子性（线程安全）

---

### 4.8 onNext 回调详解

```java
@Override
public void onNext(String token) {
    fullAnswer.append(token);
    tokenCount.incrementAndGet();
    sendSseEvent(emitter, "token", token);
}
```

**这个方法会被调用多少次？**

```
用户问题："什么是RAG？"
LLM 回答："RAG是检索增强生成技术"

onNext() 调用序列：
调用1：token = "R"
调用2：token = "AG"
调用3：token = "是"
调用4：token = "检索"
调用5：token = "增强"
调用6：token = "生成"
调用7：token = "技术"

总共调用 7 次
```

| 回答长度 | Token 数量（中文） | onNext 调用次数 |
|---------|------------------|----------------|
| 100 字 | ~150 tokens | ~150 次 |
| 500 字 | ~750 tokens | ~750 次 |
| 2000 字 | ~3000 tokens | ~3000 次 |

---

### 4.9 onComplete 回调详解

```java
@Override
public void onComplete(Response<AiMessage> response) {
    try {
        // 1. 获取完整回答
        String answer = fullAnswer.toString();
        
        // 2. 计算 Token 使用量
        Integer tokenInput = null;
        Integer tokenOutput = null;
        
        if (response.tokenUsage() != null) {
            tokenInput = response.tokenUsage().inputTokenCount();
            tokenOutput = response.tokenUsage().outputTokenCount();
        } else {
            tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
            tokenOutput = tokenCount.get();
        }

        // 3. 保存消息到数据库
        Message aiMessage = saveAssistantMessage(
                sessionId, userId, answer, citations, tokenInput, tokenOutput
        );

        // 4. 更新会话时间
        sessionMapper.updateLastMessageAt(sessionId, userId, LocalDateTime.now());

        // 5. 推送元数据
        HashMap<String, Object> metaData = new HashMap<>();
        metaData.put("messageId", aiMessage.getMessageId());
        metaData.put("tokenInput", tokenInput);
        metaData.put("tokenOutput", tokenOutput);
        metaData.put("done", true);
        
        sendSseEvent(emitter, "metaData", metaData);

        // 6. 关闭连接
        emitter.complete();

    } catch (Exception e) {
        log.error("保存流式消息失败", e);
        sendSseError(emitter, "保存消息失败");
    }
}
```

**为什么要在 onComplete 中保存消息？**

| 时刻 | 已生成内容 | 能否保存 |
|------|-----------|---------|
| onNext 第1次 | "R" | ❌ 不完整 |
| onNext 第N次 | "RAG是检索增强..." | ❌ 还在生成 |
| **onComplete** | 完整回答 | ✅ 可以保存 |

**为什么推送 metaData 事件？**

| 字段 | 前端用途 |
|------|---------|
| `messageId` | 点赞、删除、引用消息 |
| `tokenInput` | 显示"消耗了 X tokens" |
| `tokenOutput` | 计费、统计 |
| `done` | 停止显示"加载中" |

---

### 4.10 onError 回调详解

```java
@Override
public void onError(Throwable error) {
    log.error("流式LLM调用失败", error);
    sendSseError(emitter, "AI 服务暂时不可用");
}
```

**为什么不直接抛异常？**

```java
// ❌ 错误做法
@Override
public void onError(Throwable error) {
    throw new BizException("LLM调用失败");  // ← 谁来捕获？
}
```

- `onError` 在异步线程中执行
- 抛出的异常无法被 Spring 的 `@ExceptionHandler` 捕获
- 客户端已经建立 SSE 连接，只能通过 SSE 发送错误事件

---

## 5. 前端调用示例

### 5.1 使用 @microsoft/fetch-event-source（推荐）

```bash
npm install @microsoft/fetch-event-source
```

```javascript
import { fetchEventSource } from '@microsoft/fetch-event-source';

// React 示例
function ChatComponent() {
    const [answer, setAnswer] = useState('');
    const [citations, setCitations] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    const sendStreamingMessage = async (sessionId, question) => {
        setIsLoading(true);
        setAnswer('');
        setCitations([]);

        await fetchEventSource(`/rag/v1/sessions/${sessionId}/chat-stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`,
            },
            body: JSON.stringify({
                question: question,
                enableRetrieval: true,
                topK: 3,
            }),
            
            async onopen(response) {
                if (response.ok) {
                    console.log('SSE 连接建立成功');
                } else {
                    throw new Error(`连接失败: ${response.status}`);
                }
            },

            onmessage(event) {
                switch (event.event) {
                    case 'citations':
                        const citationsData = JSON.parse(event.data);
                        setCitations(citationsData);
                        break;

                    case 'token':
                        const token = JSON.parse(event.data);
                        setAnswer(prev => prev + token);
                        break;

                    case 'metaData':
                        const metaData = JSON.parse(event.data);
                        console.log('对话完成:', metaData);
                        setIsLoading(false);
                        break;

                    case 'error':
                        const error = JSON.parse(event.data);
                        console.error('错误:', error.error);
                        alert(error.error);
                        setIsLoading(false);
                        break;
                }
            },

            onerror(err) {
                console.error('SSE 错误:', err);
                setIsLoading(false);
                throw err;
            },

            onclose() {
                console.log('SSE 连接关闭');
                setIsLoading(false);
            },
        });
    };

    return (
        <div>
            <div className="answer-box">
                {answer || '等待回答...'}
                {isLoading && <span className="cursor">|</span>}
            </div>
            
            {citations.length > 0 && (
                <div className="citations">
                    <h4>引用来源：</h4>
                    {citations.map((cite, index) => (
                        <div key={index} className="citation-item">
                            <strong>{cite.docTitle}</strong> (相似度: {(cite.score * 100).toFixed(1)}%)
                            <p>{cite.sourceText}</p>
                        </div>
                    ))}
                </div>
            )}

            <button onClick={() => sendStreamingMessage(123, '什么是RAG？')}>
                发送消息
            </button>
        </div>
    );
}
```

### 5.2 使用原生 EventSource（仅支持 GET）

```javascript
// ⚠️ EventSource 原生不支持 POST，需要特殊处理
// 仅作参考，推荐使用 fetch-event-source

const eventSource = new EventSource('/rag/v1/sessions/123/chat-stream-get');

eventSource.addEventListener('token', (event) => {
    const token = JSON.parse(event.data);
    console.log('收到Token:', token);
});

eventSource.addEventListener('metaData', (event) => {
    const metaData = JSON.parse(event.data);
    console.log('完成，messageId:', metaData.messageId);
    eventSource.close();
});

eventSource.addEventListener('error', (event) => {
    console.error('错误:', event.data);
    eventSource.close();
});
```

---

## 6. 重难点解析

### 6.1 难点1：消息 ID 的时序问题

**问题：** AI 消息在流式完成后才保存到数据库，但前端已经开始接收 Token 了。

**解决方案：分两阶段推送**

```java
// 第1阶段：推送内容（逐 Token）
onNext(String token) {
    sendSseEvent(emitter, "token", token);
}

// 第2阶段：推送元数据（包含 messageId）
onComplete(Response<AiMessage> response) {
    Message aiMessage = saveAssistantMessage(...);  // 保存后才有 ID
    
    Map<String, Object> metaData = new HashMap<>();
    metaData.put("messageId", aiMessage.getMessageId());
    sendSseEvent(emitter, "metaData", metaData);
}
```

**前端处理：**
```javascript
let currentMessageId = null;

onmessage(event) {
    if (event.event === 'token') {
        // 先展示内容，messageId 暂时为空
        appendToken(event.data);
    }
    
    if (event.event === 'metaData') {
        currentMessageId = metaData.messageId;  // 最后才拿到 ID
        enableMessageActions(currentMessageId);  // 启用点赞等功能
    }
}
```

---

### 6.2 难点2：异常处理与事务边界

**问题：** 流式方法不能加 `@Transactional`

```java
@Transactional  // ❌ 错误！
public SseEmitter chatStream(...) {
    saveUserMessage(...);  // 在事务内
    
    SseEmitter emitter = new SseEmitter();
    
    // 方法返回时，事务提交
    return emitter;  // ← 此时事务已结束！
    
    // 异步线程中的操作不在事务内
    CompletableFuture.runAsync(() -> {
        saveAssistantMessage(...);  // 这个不在事务内
    });
}
```

**解决方案：**
- `saveUserMessage()` 和 `saveAssistantMessage()` 内部各自管理事务
- 流式方法本身不加 `@Transactional`

---

### 6.3 难点3：SSE 连接断开检测

**问题：** 用户关闭浏览器，但服务端还在推送。

**解决方案：**
```java
SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

// 监听超时
emitter.onTimeout(() -> {
    log.warn("SSE连接超时");
    emitter.complete();
});

// 监听完成
emitter.onCompletion(() -> {
    log.info("SSE连接正常关闭");
});

// 监听错误
emitter.onError((ex) -> {
    log.error("SSE连接异常", ex);
});

// 推送前检查连接状态
private void sendSseEvent(SseEmitter emitter, String event, Object data) {
    try {
        emitter.send(SseEmitter.event().name(event).data(data));
    } catch (IOException e) {
        // 连接已断开，停止推送
        log.warn("客户端已断开连接");
        emitter.completeWithError(e);
    }
}
```

---

### 6.4 难点4：流式模型的 Token 计算

**问题：** 流式模型的 `response.tokenUsage()` 可能为空。

**解决方案：**
```java
AtomicInteger tokenCount = new AtomicInteger(0);

onNext(String token) {
    tokenCount.incrementAndGet();  // 手动计数
}

onComplete(Response<AiMessage> response) {
    Integer tokenOutput;
    
    if (response.tokenUsage() != null) {
        tokenOutput = response.tokenUsage().outputTokenCount();
    } else {
        tokenOutput = tokenCount.get();  // 使用手动计数
    }
}
```

---

### 6.5 难点5：并发安全性

**问题：** 多个用户同时请求，`CompletableFuture` 默认使用共享线程池。

**解决方案（可选优化）：**
```java
// 自定义线程池
@Bean
public Executor chatStreamExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("chat-stream-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}

// 使用自定义线程池
CompletableFuture.runAsync(() -> {
    // 流式处理逻辑...
}, chatStreamExecutor);
```

---

## 7. 遇到的问题与解决方案

### 7.1 问题1：方法参数个数不匹配

**错误信息：**
```
Expected 6 arguments but found 7
```

**错误代码：**
```java
Message aiMessage = saveAssistantMessage(
        sessionId,
        userId,
        answer,
        userMessage.getUserId(),              // ❌ 多余参数
        buildRetrievalContextMap(citations),  // ❌ 类型错误
        promptTokens,
        completionTokens
);
```

**正确代码：**
```java
Message aiMessage = saveAssistantMessage(
        sessionId,
        userId,
        answer,
        citations,      // ✅ 直接传 List<CitationDTO>
        tokenInput,     // ✅ 统一命名
        tokenOutput
);
```

---

### 7.2 问题2：参数类型不匹配

**错误信息：**
```
Required type: List<CitationDTO>
Provided: Map<String, Object>
```

**原因：** 传入 `buildRetrievalContextMap(citations)` 返回 `Map`，但方法期望 `List<CitationDTO>`。

**解决：** 直接传 `citations` 变量。

---

### 7.3 问题3：变量命名不统一

**问题：** 流式方法使用 `promptTokens`/`completionTokens`，但 DTO 使用 `tokenInput`/`tokenOutput`。

**解决：** 统一使用 `tokenInput` 和 `tokenOutput`。

---

### 7.4 问题4：sendSseError 方法缺少错误消息

**错误代码：**
```java
private void sendSseError(SseEmitter sseEmitter, String errorMsg) {
    Map<String, Object> error = new HashMap<>();  // ❌ 空 Map
    sseEmitter.send(SseEmitter.event()
            .name("error")
            .data(error));  // ❌ 发送空数据
}
```

**正确代码：**
```java
private void sendSseError(SseEmitter sseEmitter, String errorMsg) {
    Map<String, String> error = new HashMap<>();
    error.put("error", errorMsg);  // ✅ 放入错误消息
    sseEmitter.send(SseEmitter.event()
            .name("error")
            .data(error));
    sseEmitter.completeWithError(new RuntimeException(errorMsg));  // ✅ 关闭连接
}
```

---

### 7.5 问题5：Result.isSuccess() 方法找不到

**错误信息：**
```
找不到符号: 方法 isSuccess()
```

**原因：** Maven 缓存问题，rag-common 模块的修改未被 rag-chat 识别。

**解决：**
```bash
cd D:\guide-rag
mvn clean install -DskipTests
```

---

## 8. 最佳实践总结

### 8.1 设计原则

| 原则 | 说明 |
|------|------|
| **异步前校验** | 失败能被 Spring 捕获，返回标准错误响应 |
| **异步前保存用户消息** | 即使 LLM 失败，用户输入也已记录 |
| **异步执行核心逻辑** | 释放 Tomcat 线程，提高吞吐量 |
| **检索失败不抛异常** | 降级为纯 LLM，保证服务可用性 |
| **先推送引用再推送内容** | 用户先看到"来源"，体验更好 |
| **onComplete 中保存消息** | 回答完整后才保存，避免频繁更新数据库 |
| **推送 metaData 事件** | 前端需要 messageId 进行后续操作 |
| **onError 中发送 SSE 错误** | 异步异常无法被 Spring 捕获 |

### 8.2 命名规范

| 场景 | 推荐命名 |
|------|---------|
| 输入 Token 数 | `tokenInput` |
| 输出 Token 数 | `tokenOutput` |
| SSE 事件：Token | `token` |
| SSE 事件：元数据 | `metaData` |
| SSE 事件：引用 | `citations` |
| SSE 事件：错误 | `error` |

### 8.3 错误处理

| 错误类型 | 处理方式 |
|---------|---------|
| 会话不存在 | 异步前抛 BizException，返回 4xx |
| 知识库检索失败 | 打日志，降级为纯 LLM |
| LLM 调用失败 | onError 发送 SSE error 事件 |
| 消息保存失败 | onComplete 中 catch，发送 error 事件 |
| SSE 推送失败 | completeWithError 关闭连接 |

---

## 9. 测试指南

### 9.1 启动服务

```bash
cd D:\guide-rag\rag-chat
mvn spring-boot:run
```

### 9.2 测试同步接口

**Postman / cURL：**
```bash
curl -X POST http://localhost:8083/rag/v1/sessions/123/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer xxx" \
  -d '{
    "question": "什么是RAG？",
    "enableRetrieval": true,
    "topK": 3
  }'
```

### 9.3 测试流式接口

**浏览器 Console：**
```javascript
const { fetchEventSource } = await import('https://cdn.jsdelivr.net/npm/@microsoft/fetch-event-source/lib/esm/fetch.js');

await fetchEventSource('/rag/v1/sessions/123/chat-stream', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer xxx',
    },
    body: JSON.stringify({
        question: '什么是RAG？',
        enableRetrieval: true,
    }),
    onmessage(event) {
        console.log(event.event, JSON.parse(event.data));
    }
});
```

### 9.4 验证清单

- [ ] 同步接口 `/chat` 正常返回
- [ ] 流式接口 `/chat-stream` 正常推送
- [ ] Token 逐个推送（打字机效果）
- [ ] citations 事件先于 token 事件
- [ ] metaData 事件包含 messageId
- [ ] 消息正确保存到数据库
- [ ] 会话 lastMessageAt 更新
- [ ] 检索失败时降级为纯 LLM
- [ ] LLM 失败时返回 error 事件

---

## 附录：完整流程图

```
客户端发起 POST /chat-stream
    ↓
【同步阶段（Tomcat 线程）】
├─ 1. 校验会话存在性（失败→直接返回4xx）
├─ 2. 保存用户消息到数据库
├─ 3. 创建 SseEmitter（超时5分钟）
└─ 4. 返回 SseEmitter（HTTP 200 OK, Content-Type: text/event-stream）
    ↓
【异步阶段（CompletableFuture 线程）】
├─ 4.1 调用知识库检索（Feign→rag-knowledge）
│       ├─ 成功：获得 citations 和 retrievedContext
│       └─ 失败：降级为纯 LLM（retrievedContext = null）
│
├─ 4.2 推送 citations 事件（如果有检索结果）
│       → 前端显示"正在查询文档..."
│
├─ 4.3 构建 SystemPrompt（根据有无检索上下文）
│
└─ 4.4 调用流式 LLM
        ├─ onNext() 回调（每个 Token）
        │   ├─ fullAnswer.append(token)
        │   ├─ tokenCount++
        │   └─ 推送 token 事件 → 前端逐字显示
        │
        ├─ onComplete() 回调（流式完成）
        │   ├─ 保存 AI 消息到数据库
        │   ├─ 更新会话最后消息时间
        │   ├─ 推送 metaData 事件（messageId、done=true）
        │   └─ emitter.complete() → 关闭连接
        │
        └─ onError() 回调（LLM 失败）
            ├─ 推送 error 事件
            └─ emitter.completeWithError()
```

---

**文档完成！** 🎉
