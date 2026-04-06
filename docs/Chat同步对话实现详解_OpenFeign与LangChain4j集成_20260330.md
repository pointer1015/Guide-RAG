# Chat 模块同步对话实现详解

> **文档版本**: 1.0  
> **创建日期**: 2026-03-30  
> **项目名称**: Guide-RAG 企业级知识库助手  
> **模块**: rag-chat  
> **主题**: OpenFeign + LangChain4j 实现 RAG 同步对话

---

## 📑 目录

1. [概述](#1-概述)
2. [项目背景与模块成熟度分析](#2-项目背景与模块成熟度分析)
3. [核心实现方案](#3-核心实现方案)
   - [3.1 RetrievalRequest - 检索请求 DTO](#31-retrievalrequest---检索请求-dto)
   - [3.2 RetrievalResult - 检索结果 DTO](#32-retrievalresult---检索结果-dto)
   - [3.3 KnowledgeServiceClient - OpenFeign 客户端](#33-knowledgeserviceclient---openfeign-客户端)
   - [3.4 KnowledgeServiceFallback - 服务降级处理](#34-knowledgeservicefallback---服务降级处理)
   - [3.5 LLMConfig - LangChain4j 配置](#35-llmconfig---langchain4j-配置)
   - [3.6 ChatServiceImpl - 核心实现](#36-chatserviceimpl---核心实现)
   - [3.7 SessionController - 控制器端点](#37-sessioncontroller---控制器端点)
4. [chat 方法逐步详解](#4-chat-方法逐步详解)
5. [用户提问与详细答疑](#5-用户提问与详细答疑)
6. [重要设计决策解析](#6-重要设计决策解析)
7. [遇到的问题与解决方案](#7-遇到的问题与解决方案)
8. [最佳实践总结](#8-最佳实践总结)
9. [测试指南](#9-测试指南)

---

## 1. 概述

### 1.1 什么是 RAG？

**RAG (Retrieval-Augmented Generation)** 即"检索增强生成"，是一种结合检索和生成的 AI 技术：

1. **检索阶段**：根据用户问题，从知识库中检索相关文档片段
2. **生成阶段**：将检索到的内容作为上下文，结合用户问题一起发送给 LLM
3. **输出阶段**：LLM 基于上下文生成有据可循的回答

### 1.2 为什么需要 RAG？

| 对比维度 | 纯 LLM | RAG |
|---------|-------|-----|
| **知识时效性** | 训练数据截止日期后的知识不知道 | 可以检索最新文档 |
| **专业领域知识** | 通用知识，缺乏企业私有知识 | 可以接入企业知识库 |
| **回答可信度** | 可能产生幻觉（编造信息） | 基于真实文档，可追溯来源 |
| **数据隐私** | 需要将数据发送给 LLM 训练 | 数据留在本地，只发送检索结果 |

### 1.3 本模块实现的功能

```
用户发送问题
    ↓
1. 校验会话 + 保存用户消息
    ↓
2. 调用知识库检索（OpenFeign → rag-knowledge）
    ↓
3. 构建 Prompt（系统提示 + 检索上下文 + 用户问题）
    ↓
4. 调用 LLM 生成回答（LangChain4j）
    ↓
5. 保存 AI 回答消息
    ↓
6. 返回响应（包含回答 + 引用来源）
```

---

## 2. 项目背景与模块成熟度分析

### 2.1 项目结构

```
guide-rag/
├── rag-common/      # 公共模块（Result、异常、上下文）
├── rag-gateway/     # API 网关
├── rag-auth/        # 认证授权
├── rag-knowledge/   # 知识库服务（文档、检索）
└── rag-chat/        # 对话服务（会话、消息、Chat）
```

### 2.2 模块成熟度评估

| 模块 | 完成度 | 说明 |
|------|-------|------|
| `rag-auth` | 95% | 认证链路较完整 |
| `rag-chat` CRUD | 95% | 会话/消息 CRUD 已完善 |
| `rag-chat` Chat | **30% → 100%** | 本次实现的核心内容 |
| `rag-knowledge` | 70% | 知识库基础能力有进展 |
| `rag-gateway` | 60% | 框架存在，治理能力需补齐 |

### 2.3 优先级决策

经过分析，确定优先补齐 `rag-chat` 的 Chat 主链路：

1. ✅ 同步 Chat 接口完整跑通
2. ✅ 接入检索服务（OpenFeign）
3. ✅ 接入大模型（LangChain4j）
4. ✅ 保证引用可追溯（Citation）
5. ⏳ 再推进 SSE 流式（见另一份文档）

---

## 3. 核心实现方案

### 3.1 RetrievalRequest - 检索请求 DTO

```java
package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索请求 DTO
 * 用于调用 rag-knowledge 服务的向量检索接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库检索请求")
public class RetrievalRequest {

    /**
     * 检索查询文本
     * 通常是用户的问题
     */
    @Schema(description = "检索查询文本", example = "如何配置 Spring Boot 数据源？")
    private String query;

    /**
     * 返回的最大结果数量
     * 默认 5，最大 20
     */
    @Schema(description = "返回结果数量", example = "5", defaultValue = "5")
    private Integer topK;

    /**
     * 最低相似度分数阈值
     * 低于此分数的结果将被过滤
     */
    @Schema(description = "最低相似度阈值", example = "0.5", defaultValue = "0.5")
    private Double minScore;
}
```

**设计要点：**
- 使用 `@Builder` 支持链式构建
- `topK` 和 `minScore` 有默认值，调用方可选择性覆盖
- `query` 是用户问题的原文，用于向量检索

---

### 3.2 RetrievalResult - 检索结果 DTO

```java
package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 检索结果 DTO
 * 封装 rag-knowledge 服务返回的检索结果
 */
@Data
@Schema(description = "知识库检索结果")
public class RetrievalResult {

    /**
     * 检索到的文档块列表
     */
    @Schema(description = "检索到的文档块列表")
    private List<ChunkResult> chunks;

    /**
     * 检索耗时（毫秒）
     */
    @Schema(description = "检索耗时(ms)", example = "150")
    private Long latencyMs;

    /**
     * 文档块结果
     */
    @Data
    @Schema(description = "文档块详情")
    public static class ChunkResult {

        /**
         * 文档 ID
         */
        @Schema(description = "文档ID", example = "1912345678901234567")
        private Long documentId;

        /**
         * 文档标题
         */
        @Schema(description = "文档标题", example = "Spring Boot 配置指南.pdf")
        private String documentTitle;

        /**
         * 分块 ID
         */
        @Schema(description = "分块ID", example = "1912345678901234568")
        private Long chunkId;

        /**
         * 分块序号（在文档中的位置）
         */
        @Schema(description = "分块序号", example = "3")
        private Integer chunkIndex;

        /**
         * 分块内容
         */
        @Schema(description = "分块内容", example = "数据源配置需要在 application.yml 中添加...")
        private String content;

        /**
         * 相似度分数（0-1）
         */
        @Schema(description = "相似度分数", example = "0.92")
        private Double score;
    }
}
```

**设计要点：**
- 使用内部类 `ChunkResult` 封装单个文档块信息
- `score` 表示与用户问题的相似度，用于排序和过滤
- `chunkIndex` 用于定位文档中的具体位置

---

### 3.3 KnowledgeServiceClient - OpenFeign 客户端

```java
package com.guiderag.chat.client;

import com.guiderag.chat.model.dto.RetrievalRequest;
import com.guiderag.chat.model.dto.RetrievalResult;
import com.guiderag.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 知识库服务 Feign 客户端
 * 
 * 通过 OpenFeign 调用 rag-knowledge 服务的检索接口
 */
@FeignClient(
        name = "rag-knowledge",           // 服务名称（Nacos 注册名）
        path = "/rag/v1",                 // 统一路径前缀
        fallback = KnowledgeServiceFallback.class  // 降级处理类
)
public interface KnowledgeServiceClient {

    /**
     * 向量检索
     * 
     * @param knowledgeBaseId 知识库 ID
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/retrieve")
    Result<RetrievalResult> retrieve(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId,
            @RequestBody RetrievalRequest request
    );
}
```

**设计要点：**

| 注解/参数 | 作用 |
|----------|------|
| `name = "rag-knowledge"` | 指定目标服务名称（从 Nacos 获取实例列表） |
| `path = "/rag/v1"` | 统一路径前缀，避免每个方法重复写 |
| `fallback = ...` | 指定降级处理类，服务不可用时执行 |

---

### 3.4 KnowledgeServiceFallback - 服务降级处理

```java
package com.guiderag.chat.client;

import com.guiderag.chat.model.dto.RetrievalRequest;
import com.guiderag.chat.model.dto.RetrievalResult;
import com.guiderag.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 知识库服务降级处理
 * 
 * 当 rag-knowledge 服务不可用时，提供降级逻辑：
 * - 返回空的检索结果
 * - 允许 Chat 继续执行（降级为纯 LLM 对话）
 * 
 * 体现微服务"可降级优先"的可靠性原则
 */
@Slf4j
@Component
public class KnowledgeServiceFallback implements KnowledgeServiceClient {

    @Override
    public Result<RetrievalResult> retrieve(Long knowledgeBaseId, RetrievalRequest request) {
        log.warn("知识库服务不可用，触发降级处理，knowledgeBaseId={}, query={}",
                knowledgeBaseId, request.getQuery());

        // 返回空的检索结果，而不是抛出异常
        RetrievalResult emptyResult = new RetrievalResult();
        emptyResult.setChunks(Collections.emptyList());
        emptyResult.setLatencyMs(0L);

        return Result.success(emptyResult);
    }
}
```

**为什么要有 Fallback？**

| 场景 | 无 Fallback | 有 Fallback |
|------|------------|-------------|
| rag-knowledge 正常 | ✅ 返回检索结果 | ✅ 返回检索结果 |
| rag-knowledge 超时 | ❌ 抛异常，Chat 失败 | ✅ 返回空结果，Chat 继续（纯 LLM） |
| rag-knowledge 宕机 | ❌ 抛异常，Chat 失败 | ✅ 返回空结果，Chat 继续（纯 LLM） |

**核心原则：**
- 检索是"增强"，不是"必要条件"
- 检索失败不应该阻断用户对话
- 降级为纯 LLM 对话，用户仍能得到回答

---

### 3.5 LLMConfig - LangChain4j 配置

```java
package com.guiderag.chat.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 模型配置
 * 支持 DeepSeek 和 OpenAI（通过配置切换）
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

    /**
     * 聊天模型 Bean
     * 根据配置选择 DeepSeek 或 OpenAI
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化LLM模型: provider={}, timeout={}s", provider, timeoutSeconds);

        if ("openai".equalsIgnoreCase(provider)) {
            return buildOpenAiModel(openaiApiKey, openaiBaseUrl, openaiModel);
        } else {
            // 默认使用 DeepSeek（兼容 OpenAI API）
            return buildOpenAiModel(deepseekApiKey, deepseekBaseUrl, deepseekModel);
        }
    }

    /**
     * 构建 OpenAI 兼容模型
     * 
     * DeepSeek 使用 OpenAI 兼容接口，所以可以复用同一个构建方法
     * 只需要更换 baseUrl 和 apiKey 即可
     */
    private ChatLanguageModel buildOpenAiModel(String apiKey, String baseUrl, String model) {
        log.info("构建 OpenAI 兼容模型：baseUrl={}, model={}", baseUrl, model);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)                              // API 密钥
                .baseUrl(baseUrl)                            // API 基础 URL
                .modelName(model)                            // 模型名称
                .temperature(0.5)                            // 温度参数（控制随机性）
                .timeout(Duration.ofSeconds(timeoutSeconds)) // 超时时间
                .maxRetries(2)                               // 重试次数
                .logRequests(true)                           // 记录请求日志
                .logResponses(true)                          // 记录响应日志
                .build();
    }
}
```

**buildOpenAiModel 参数详解：**

| 参数 | 作用 | 推荐值 |
|------|------|-------|
| `apiKey` | API 认证密钥 | 从环境变量读取 |
| `baseUrl` | API 基础 URL | DeepSeek: `https://api.deepseek.com` |
| `modelName` | 模型名称 | `deepseek-chat` 或 `gpt-4o` |
| `temperature` | 生成随机性（0-1） | 0.5（平衡创造性和准确性） |
| `timeout` | 请求超时 | 60 秒（LLM 生成较慢） |
| `maxRetries` | 重试次数 | 2（瞬时故障可恢复） |
| `logRequests` | 记录请求日志 | 开发环境 true，生产 false |
| `logResponses` | 记录响应日志 | 开发环境 true，生产 false |

---

### 3.6 ChatServiceImpl - 核心实现

```java
package com.guiderag.chat.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.guiderag.chat.client.KnowledgeServiceClient;
import com.guiderag.chat.mapper.MessageMapper;
import com.guiderag.chat.mapper.SessionMapper;
import com.guiderag.chat.model.dto.ChatReqDTO;
import com.guiderag.chat.model.dto.ChatResDTO;
import com.guiderag.chat.model.dto.RetrievalRequest;
import com.guiderag.chat.model.dto.RetrievalResult;
import com.guiderag.chat.model.entity.Message;
import com.guiderag.chat.model.entity.Session;
import com.guiderag.chat.service.ChatService;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import com.guiderag.common.result.Result;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final KnowledgeServiceClient knowledgeServiceClient;
    private final ChatLanguageModel chatLanguageModel;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResDTO chat(Long sessionId, ChatReqDTO dto) {
        long startTime = System.currentTimeMillis();

        // ========== 第1步：校验会话 ==========
        Long userId = UserContextHolder.getUserId();
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        // ========== 第2步：保存用户消息 ==========
        Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());
        log.info("用户消息已保存, messageId={}", userMessage.getMessageId());

        // ========== 第3步：检索知识库 ==========
        List<ChatResDTO.CitationDTO> citations = new ArrayList<>();
        String retrievedContext = "";

        if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && session.getKnowledgeBaseId() != null) {
            try {
                RetrievalRequest retrievalRequest = RetrievalRequest.builder()
                        .query(dto.getQuestion())
                        .topK(dto.getTopK() != null ? dto.getTopK() : 5)
                        .minScore(0.5)
                        .build();

                log.info("开始向量检索，knowledgeBaseId={}", session.getKnowledgeBaseId());
                Result<RetrievalResult> retrievalResult = knowledgeServiceClient
                        .retrieve(session.getKnowledgeBaseId(), retrievalRequest);

                if (retrievalResult != null && retrievalResult.isSuccess() && retrievalResult.getData() != null) {
                    RetrievalResult result = retrievalResult.getData();
                    if (result.getChunks() != null && !result.getChunks().isEmpty()) {
                        citations = convertToCitations(result.getChunks());
                        retrievedContext = buildRetrievalContext(result.getChunks());
                    }
                }
            } catch (Exception e) {
                log.error("向量检索失败，降级为纯LLM对话", e);
                // 不抛异常，继续执行
            }
        }

        // ========== 第4步：调用 LLM ==========
        String systemPrompt = buildSystemPrompt(retrievedContext);
        String answer;
        int tokenInput = 0;
        int tokenOutput = 0;

        try {
            Response<AiMessage> response = chatLanguageModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(dto.getQuestion())
            );

            answer = response.content().text();

            if (response.tokenUsage() != null) {
                tokenInput = response.tokenUsage().inputTokenCount();
                tokenOutput = response.tokenUsage().outputTokenCount();
            } else {
                tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
                tokenOutput = estimateTokens(answer);
            }
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            throw new BizException("B0001", "AI 回答生成失败：" + e.getMessage());
        }

        // ========== 第5步：保存 AI 消息 ==========
        Message assistantMessage = saveAssistantMessage(
                sessionId, userId, answer, citations, tokenInput, tokenOutput
        );

        // ========== 第6步：更新会话时间 ==========
        sessionMapper.updateLastMessageAt(sessionId, userId, LocalDateTime.now());

        // ========== 第7步：构建响应 ==========
        long latencyMs = System.currentTimeMillis() - startTime;

        ChatResDTO response = new ChatResDTO();
        response.setMessageId(assistantMessage.getMessageId());
        response.setAnswer(answer);
        response.setCitations(citations);
        response.setTokenInput(tokenInput);
        response.setTokenOutput(tokenOutput);
        response.setLatencyMs((int) latencyMs);
        response.setGmtCreate(assistantMessage.getGmtCreate());

        log.info("Chat 完成, sessionId={}, latencyMs={}, citations={}",
                sessionId, latencyMs, citations.size());

        return response;
    }

    // ==================== 辅助方法 ====================

    /**
     * 保存用户消息
     */
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

    /**
     * 保存 AI 回答消息
     */
    private Message saveAssistantMessage(Long sessionId, Long userId, String answer,
                                         List<ChatResDTO.CitationDTO> citations,
                                         int tokenInput, int tokenOutput) {
        // 构建引用上下文
        HashMap<String, Object> retrievalContext = new HashMap<>();
        retrievalContext.put("citations", citations);

        // 提取 chunkId 列表
        List<String> chunkIds = citations.stream()
                .map(ChatResDTO.CitationDTO::getChunkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Message message = new Message();
        message.setMessageId(IdUtil.getSnowflakeNextId());
        message.setTenantId(userId);
        message.setSessionId(sessionId);
        message.setUserId(null);  // assistant 消息无 userId
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

    /**
     * 转换检索结果为引用信息
     */
    private List<ChatResDTO.CitationDTO> convertToCitations(List<RetrievalResult.ChunkResult> chunks) {
        return chunks.stream().map(chunk -> {
            ChatResDTO.CitationDTO citation = new ChatResDTO.CitationDTO();
            citation.setDocId(chunk.getDocumentId() != null ? chunk.getDocumentId().toString() : null);
            citation.setDocTitle(chunk.getDocumentTitle());
            citation.setChunkId(chunk.getChunkId() != null ? chunk.getChunkId().toString() : null);
            citation.setChunkIndex(chunk.getChunkIndex());
            citation.setScore(chunk.getScore());
            citation.setSourceText(chunk.getContent());
            return citation;
        }).collect(Collectors.toList());
    }

    /**
     * 构建检索上下文（给 LLM 的参考资料）
     */
    private String buildRetrievalContext(List<RetrievalResult.ChunkResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalResult.ChunkResult chunk = chunks.get(i);
            context.append(String.format("[文档%d] %s\n%s\n\n",
                    i + 1,
                    chunk.getDocumentTitle() != null ? chunk.getDocumentTitle() : "未知文档",
                    chunk.getContent()
            ));
        }
        return context.toString();
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String retrievedContext) {
        if (retrievedContext == null || retrievedContext.isEmpty()) {
            return "你是一个专业、友好的AI助手。请根据用户的问题提供准确、有帮助的回答。";
        }

        return "你是一个专业的知识库助手。请根据以下检索到的相关文档，回答用户的问题。\n\n" +
                "【检索到的相关文档】\n" + retrievedContext + "\n" +
                "【要求】\n" +
                "1. 优先基于上述文档内容回答\n" +
                "2. 如果文档中没有相关信息，可以结合你的知识补充\n" +
                "3. 回答要准确、简洁、有条理\n" +
                "4. 如果引用了文档内容，请自然地融入回答中";
    }

    /**
     * 估算 Token 数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 统计中文字符
        long chineseChars = text.chars()
                .filter(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)
                .count();

        // 统计英文单词
        long englishWords = Arrays.stream(text.split("\\s+"))
                .filter(word -> word.matches(".*[a-zA-Z].*"))
                .count();

        // 中文约 1.5 token/字，英文约 0.25 token/词
        return (int) (chineseChars * 1.5 + englishWords * 0.25);
    }
}
```

---

### 3.7 SessionController - 控制器端点

```java
/**
 * 发送消息（同步版本）
 */
@Operation(
        summary = "发送消息（同步）",
        description = "向指定会话发送消息，同步返回完整 AI 回答。包含知识库检索和引用来源。"
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
})
@PostMapping("/{sessionId}/chat")
public Result<ChatResDTO> chat(
        @Parameter(description = "会话ID", required = true)
        @PathVariable Long sessionId,
        @Valid @RequestBody ChatReqDTO dto) {

    ChatResDTO response = chatService.chat(sessionId, dto);
    return Result.success(response);
}
```

---

## 4. chat 方法逐步详解

### 4.1 第1步：校验会话

```java
Long userId = UserContextHolder.getUserId();
Session session = sessionMapper.selectById(sessionId, userId);
if (session == null) {
    throw new BizException("A0500", "会话不存在或无权访问");
}
```

**为什么这样做？**

1. **用户隔离**：`UserContextHolder.getUserId()` 从 JWT 解析当前用户
2. **归属校验**：`selectById(sessionId, userId)` 确保会话属于当前用户
3. **快速失败**：会话不存在或无权限时，立即返回错误，不浪费后续资源

**安全意义：**
- 防止用户 A 访问用户 B 的会话
- 防止通过枚举 sessionId 遍历所有会话

---

### 4.2 第2步：保存用户消息

```java
Message userMessage = saveUserMessage(sessionId, userId, dto.getQuestion());
```

**为什么要先保存用户消息？**

| 保存时机 | 如果 LLM 失败 | 用户消息状态 |
|---------|--------------|-------------|
| **先保存（当前做法）** | AI 消息不保存 | ✅ 用户消息已保存 |
| **后保存** | 整个流程失败 | ❌ 用户消息丢失 |

**业务意义：**
- 用户消息是**用户的输入**，必须可靠保存
- 即使 AI 回答失败，用户也能看到自己发的消息
- 支持审计、历史记录、问题排查

---

### 4.3 第3步：检索知识库

```java
if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && session.getKnowledgeBaseId() != null) {
    try {
        // 检索逻辑...
    } catch (Exception e) {
        log.error("向量检索失败，降级为纯LLM对话", e);
        // 不抛异常！
    }
}
```

**条件解析：**

| 条件 | 含义 |
|------|------|
| `Boolean.TRUE.equals(dto.getEnableRetrieval())` | 用户主动启用检索（null-safe 写法） |
| `session.getKnowledgeBaseId() != null` | 会话绑定了知识库 |

**为什么检索失败不抛异常？**

这是**降级策略（Graceful Degradation）**的体现：

| 场景 | 检索状态 | 处理方式 |
|------|---------|---------|
| 知识库可用 | ✅ 返回文档 | RAG 模式：LLM + 检索上下文 |
| 知识库不可用 | ❌ 超时/异常 | **降级为纯 LLM 模式** |

**原则：** 检索是"增强"，不是"必要条件"

---

### 4.4 第4步：调用 LLM

```java
try {
    Response<AiMessage> response = chatLanguageModel.generate(
            SystemMessage.from(systemPrompt),
            UserMessage.from(dto.getQuestion())
    );

    answer = response.content().text();

    if (response.tokenUsage() != null) {
        tokenInput = response.tokenUsage().inputTokenCount();
        tokenOutput = response.tokenUsage().outputTokenCount();
    } else {
        tokenInput = estimateTokens(systemPrompt + dto.getQuestion());
        tokenOutput = estimateTokens(answer);
    }
} catch (Exception e) {
    log.error("LLM 调用失败", e);
    throw new BizException("B0001", "AI 回答生成失败：" + e.getMessage());
}
```

**为什么 LLM 失败要抛异常？**

| 组件 | 失败处理 | 原因 |
|------|---------|------|
| 检索 | 降级（不抛异常） | 检索是增强，非必要 |
| **LLM** | **抛异常** | LLM 是核心输出，失败必须显式报错 |

**如果 LLM 失败不抛异常会怎样？**
- 用户发送了问题
- AI 没有回答
- 用户不知道发生了什么
- 体验极差

**Response 结构解析：**

```java
Response<AiMessage>
├── content()      → AiMessage
│   └── text()     → String（回答文本）
├── tokenUsage()   → TokenUsage（可能为 null）
│   ├── inputTokenCount()  → int
│   └── outputTokenCount() → int
└── finishReason() → FinishReason（完成原因）
```

---

### 4.5 第5步：保存 AI 消息

```java
Message assistantMessage = saveAssistantMessage(
        sessionId, userId, answer, citations, tokenInput, tokenOutput
);
```

**保存的内容：**

| 字段 | 值 | 用途 |
|------|-----|------|
| `role` | "assistant" | 标识为 AI 消息 |
| `content` | 回答文本 | 展示给用户 |
| `referencedChunkIds` | chunkId 列表 | 引用追溯 |
| `retrievalContext` | 完整引用信息 | 审计快照 |
| `tokenInput` | 输入 token 数 | 计费统计 |
| `tokenOutput` | 输出 token 数 | 计费统计 |

---

### 4.6 第6步：更新会话时间

```java
sessionMapper.updateLastMessageAt(sessionId, userId, LocalDateTime.now());
```

**为什么要更新会话时间？**

1. **会话列表排序**：最近活跃的会话排在前面
2. **活跃度分析**：统计用户使用频率
3. **过期清理**：长期不活跃的会话可以清理

---

### 4.7 第7步：构建响应

```java
ChatResDTO response = new ChatResDTO();
response.setMessageId(assistantMessage.getMessageId());
response.setAnswer(answer);
response.setCitations(citations);
response.setTokenInput(tokenInput);
response.setTokenOutput(tokenOutput);
response.setLatencyMs((int) latencyMs);
response.setGmtCreate(assistantMessage.getGmtCreate());
```

**响应字段说明：**

| 字段 | 用途 |
|------|------|
| `messageId` | 用于后续操作（点赞、删除等） |
| `answer` | 展示给用户的回答 |
| `citations` | 引用来源（前端展示"参考文档"） |
| `tokenInput` | 计费、统计 |
| `tokenOutput` | 计费、统计 |
| `latencyMs` | 性能监控 |
| `gmtCreate` | 时间展示 |

---

## 5. 用户提问与详细答疑

### Q1: KnowledgeServiceFallback.class 是什么？

**原始问题：**
> "KnowledgeServiceFallback.class 这是什么，貌似还没创建？"

**答案：**
- `.class` 是 Java 语法，表示"类对象引用"，不是文件后缀
- `KnowledgeServiceFallback.class` 等价于 `Class<KnowledgeServiceFallback>` 类型的对象
- 该类必须存在并实现 `KnowledgeServiceClient` 接口

**类比：**
```java
String.class    // Class<String> 对象
Integer.class   // Class<Integer> 对象
KnowledgeServiceFallback.class  // Class<KnowledgeServiceFallback> 对象
```

---

### Q2: Cannot resolve method 'buildOpenAiModel' 怎么解决？

**原始问题：**
> "Cannot resolve method 'buildOpenAiModel' in 'LLMConfig' 这个报错如何解决？"

**根因分析：**
- `chatLanguageModel()` 中调用了 `buildOpenAiModel(...)`
- 但类中未定义该私有方法，或复制代码时遗漏

**修复步骤：**
1. 在 `LLMConfig` 类中添加 `buildOpenAiModel` 方法
2. 确保 import 完整（`OpenAiChatModel`、`Duration`）
3. 刷新 Maven 依赖：`mvn clean install -DskipTests`
4. IDE 中执行 "Invalidate Caches and Restart"

---

### Q3: buildOpenAiModel 方法每个参数是什么意思？

**原始问题：**
> "解析这个方法（buildOpenAiModel）。"

**详细解答见 3.5 节，核心参数表：**

| 参数 | 作用 | 为什么这样设置 |
|------|------|--------------|
| `apiKey` | API 认证 | 每个供应商都需要 |
| `baseUrl` | API 入口 | DeepSeek 和 OpenAI URL 不同 |
| `modelName` | 模型版本 | 不同模型能力和价格不同 |
| `temperature` | 随机性 | 0.5 平衡创造性和准确性 |
| `timeout` | 超时保护 | LLM 生成较慢，60秒合理 |
| `maxRetries` | 瞬时故障重试 | 2次重试覆盖大部分瞬时错误 |
| `logRequests/logResponses` | 可观测性 | 开发调试用 |

---

### Q4: 为什么用 HashMap 存储 retrievalContext？

**原始问题：**
> "Map<String, Object> retrievalContext = new HashMap<>(); 为什么用 HashMap？"

**答案：**
- 数据库字段 `retrieval_context` 是 **JSON/JSONB** 类型
- 需要存储为 JSON 对象（`{}`），而非数组（`[]`）
- HashMap 序列化后正好是 `{"citations": [...], "query": "...", ...}`

**为什么不直接存 citations 列表？**

```json
// 直接存列表（不推荐）
[{...}, {...}]  // 缺少顶层语义键

// 用 HashMap（推荐）
{
  "citations": [{...}, {...}],
  "query": "原始问题",
  "topK": 5,
  "retrievalLatency": 150
}
```

**好处：**
1. 有明确的语义键（`citations`）
2. 便于扩展（后续加 `query`、`topK` 等）
3. 便于查询（`WHERE retrieval_context->>'query' = '...'`）

---

### Q5: chunkIds 是什么意思？

**原始问题：**
> "chunkIds 是什么意思？"

**答案：**
- `chunkIds` 是"本次回答引用到的知识分块 ID 列表"
- 对应 `document_chunk` 表的主键

**价值：**
1. **回答可追溯**：知道 AI 基于哪些文档片段回答
2. **统计分析**：哪些文档块被高频引用
3. **前端跳转**：点击引用可跳转到原文位置

---

### Q6: convertToCitations 方法详细解析

**原始问题：**
> "详细解析 convertToCitations 方法。"

```java
private List<ChatResDTO.CitationDTO> convertToCitations(List<RetrievalResult.ChunkResult> chunks) {
    return chunks.stream().map(chunk -> {
        ChatResDTO.CitationDTO citation = new ChatResDTO.CitationDTO();
        citation.setDocId(chunk.getDocumentId() != null ? chunk.getDocumentId().toString() : null);
        citation.setDocTitle(chunk.getDocumentTitle());
        citation.setChunkId(chunk.getChunkId() != null ? chunk.getChunkId().toString() : null);
        citation.setChunkIndex(chunk.getChunkIndex());
        citation.setScore(chunk.getScore());
        citation.setSourceText(chunk.getContent());
        return citation;
    }).collect(Collectors.toList());
}
```

**它在做什么？**
- 把检索层返回的内部结构（`ChunkResult`）映射成 API 返回结构（`CitationDTO`）

**为什么要转换？**

| 原因 | 说明 |
|------|------|
| **边界清晰** | 内部模型不直接暴露给前端 |
| **字段语义对齐** | `content` → `sourceText` 更符合引用语境 |
| **类型安全** | Long ID 转 String，避免 JS 大整数精度丢失 |
| **契约稳定** | 检索层变动时，接口层可通过转换层隔离影响 |

---

### Q7: Result 和 RetrievalResult 有什么关系？

**原始问题：**
> "RetrievalResult 和 Result 有关联吗？"

**答案：有，是泛型包装关系。**

```java
Result<RetrievalResult>
│
├── code: 200
├── message: "success"
└── data: RetrievalResult
         ├── chunks: [...]
         └── latencyMs: 150
```

**类比：**
- `Result` = 快递包装盒（统一格式）
- `RetrievalResult` = 快递内容物（业务数据）

**设计意义：**
- 统一响应格式，所有接口都返回 `Result<T>`
- 业务数据通过泛型 `T` 传递
- 前端只需要统一处理 `code`、`message`、`data`

---

### Q8: 为什么判定条件是 Boolean.TRUE.equals(...)？

**原始问题：**
> "if (Boolean.TRUE.equals(dto.getEnableRetrieval()) && session.getKnowledgeBaseId() != null) 为什么判定条件是这个？"

**Boolean.TRUE.equals() vs 直接判断：**

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
| `null` | ⚠️ NPE | ❌ false（安全） |

**完整条件逻辑：**
- `Boolean.TRUE.equals(dto.getEnableRetrieval())`：用户主动启用检索
- `session.getKnowledgeBaseId() != null`：会话绑定了知识库

**两个条件都满足才检索，否则跳过。**

---

### Q9: 为什么要将检索结果转换为引用消息？

**原始问题：**
> "为什么要将检索结果转换为引用消息？"

**答案：检索结果有两个用途。**

**用途1：给 LLM 用（构建 Prompt）**
```java
retrievedContext = buildRetrievalContext(result.getChunks());
// 变成：
// [文档1] Spring Boot 配置指南.pdf
// 数据源配置需要在 application.yml 中添加...
//
// [文档2] ...
```

**用途2：给前端用（展示引用来源）**
```java
citations = convertToCitations(result.getChunks());
// 变成：
// [{docTitle: "Spring Boot 配置指南.pdf", sourceText: "...", score: 0.92}, ...]
```

**RAG 产品的核心价值：**
- 不仅要"答得出"，还要"说得清来源"
- 用户可以点击引用查看原文
- 增强回答的可信度

---

### Q10: 大模型返回的消息是怎样的？

**原始问题：**
> "大模型返回的消息是怎样的？为什么要单独提取回答内容？"

**Response 结构：**
```java
Response<AiMessage>
├── content: AiMessage
│   └── text: "RAG 是检索增强生成..."  // 回答文本
├── tokenUsage: TokenUsage (可能为 null)
│   ├── inputTokenCount: 256
│   └── outputTokenCount: 512
└── finishReason: STOP  // 完成原因
```

**为什么要提取 `.text()`？**
- 业务需要的是"可展示的文本内容"
- 不是整个 Response 对象
- Token 等元信息会单独保存到字段

---

## 6. 重要设计决策解析

### 6.1 为什么加 @Transactional？

```java
@Transactional(rollbackFor = Exception.class)
public ChatResDTO chat(Long sessionId, ChatReqDTO dto) {
```

**作用：** 保证以下操作的原子性：
1. 保存用户消息
2. 保存 AI 消息
3. 更新会话时间

**如果不加 @Transactional：**
- 用户消息保存成功
- AI 消息保存失败
- 结果：用户消息在，AI 消息不在（数据不一致）

**rollbackFor = Exception.class：**
- 默认只有 RuntimeException 才回滚
- 加上这个后，所有 Exception 都会回滚

---

### 6.2 检索 vs LLM 的错误处理差异

| 组件 | 失败处理 | 原因 |
|------|---------|------|
| **检索** | try-catch + 日志 | 检索是增强，非必要 |
| **LLM** | 抛 BizException | LLM 是核心，失败必须报错 |

**代码体现：**
```java
// 检索失败 - 降级
try {
    // 检索逻辑...
} catch (Exception e) {
    log.error("检索失败", e);
    // 继续执行，不抛异常
}

// LLM 失败 - 报错
try {
    // LLM 调用...
} catch (Exception e) {
    throw new BizException("AI 回答生成失败");
}
```

---

### 6.3 Long ID 转 String 的原因

```java
citation.setDocId(chunk.getDocumentId() != null ? chunk.getDocumentId().toString() : null);
```

**为什么要转 String？**
- JavaScript 的 Number 类型只有 53 位精度
- Java Long 是 64 位
- 超过 `2^53` 的整数在 JS 中会丢失精度

**示例：**
```javascript
// Java Long
1912345678901234567L

// JavaScript Number（精度丢失）
1912345678901234560  // 最后几位变了！

// JavaScript String（安全）
"1912345678901234567"  // 完整保留
```

---

## 7. 遇到的问题与解决方案

### 7.1 Result.isSuccess() 方法不存在

**错误信息：**
```
找不到符号: 方法 isSuccess()
```

**原因：** `Result` 类原本只有 `code`、`message`、`data` 和静态工厂方法。

**解决方案：在 Result 类中添加方法：**
```java
public boolean isSuccess() {
    return this.code == ResultCode.SUCCESS.getCode();
}
```

---

### 7.2 buildOpenAiModel 方法不存在

**错误信息：**
```
Cannot resolve method 'buildOpenAiModel'
```

**原因：** 复制代码时遗漏了私有方法。

**解决方案：** 补全 `LLMConfig` 类中的 `buildOpenAiModel` 方法。

---

### 7.3 Fallback 类未被扫描

**错误信息：**
```
No qualifying bean of type 'KnowledgeServiceFallback'
```

**解决方案：**
1. 确保类上有 `@Component` 注解
2. 确保类在组件扫描路径下
3. 确保实现了 `KnowledgeServiceClient` 接口

---

### 7.4 Feign 调用超时

**错误信息：**
```
Read timed out
```

**解决方案：在 application.yml 中配置超时：**
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 30000
```

---

## 8. 最佳实践总结

### 8.1 设计原则

| 原则 | 说明 |
|------|------|
| **先鉴权再执行业务** | 会话归属校验优先 |
| **先落用户消息** | 保障审计与可追踪 |
| **检索可失败但不阻断** | 降级优先保证可用性 |
| **LLM 失败必须显式报错** | 核心输出失败不能静默 |
| **回答与引用同时落库** | 可回放、可追溯 |
| **更新会话活跃时间** | 支撑会话列表排序 |
| **统一响应 DTO** | 前后端契约稳定 |

### 8.2 命名规范

| 场景 | 推荐命名 |
|------|---------|
| 输入 Token 数 | `tokenInput` |
| 输出 Token 数 | `tokenOutput` |
| 检索结果 | `retrievalResult` |
| 引用列表 | `citations` |
| 检索上下文 | `retrievedContext` |

### 8.3 错误处理

| 错误类型 | 处理方式 |
|---------|---------|
| 会话不存在 | 抛 BizException，返回 4xx |
| 知识库检索失败 | 打日志，降级为纯 LLM |
| LLM 调用失败 | 抛 BizException，返回 5xx |
| Token 统计为空 | 使用估算值 |

---

## 9. 测试指南

### 9.1 启动服务

```bash
# 设置环境变量
set DEEPSEEK_API_KEY=sk-your-api-key

# 编译
cd D:\guide-rag
mvn clean install -DskipTests

# 启动
cd rag-chat
mvn spring-boot:run
```

### 9.2 测试接口

**Postman / cURL：**
```bash
curl -X POST http://localhost:8083/rag/v1/sessions/123/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer xxx" \
  -d '{
    "question": "什么是RAG？",
    "enableRetrieval": true,
    "topK": 5
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "messageId": "1912345678901234567",
    "answer": "RAG（Retrieval-Augmented Generation）是检索增强生成...",
    "citations": [
      {
        "docId": "123",
        "docTitle": "RAG技术白皮书.pdf",
        "chunkId": "456",
        "chunkIndex": 3,
        "score": 0.92,
        "sourceText": "RAG 是一种结合检索和生成的..."
      }
    ],
    "tokenInput": 256,
    "tokenOutput": 512,
    "latencyMs": 1500,
    "gmtCreate": "2026-03-30T10:00:00"
  }
}
```

### 9.3 验证清单

- [ ] 接口正常返回
- [ ] 用户消息正确保存
- [ ] AI 消息正确保存
- [ ] 检索结果包含在 citations 中
- [ ] 检索失败时降级为纯 LLM
- [ ] LLM 失败时返回错误码
- [ ] Token 统计正确
- [ ] 会话时间更新

---

## 附录：完整流程图

```
客户端发起 POST /sessions/{sessionId}/chat
    ↓
【第1步】校验会话
├─ 获取当前用户 ID（从 JWT）
├─ 查询会话（校验归属）
└─ 失败 → 返回 "会话不存在或无权访问"
    ↓
【第2步】保存用户消息
├─ 生成消息 ID（雪花算法）
├─ 设置 role = "user"
└─ 插入 message 表
    ↓
【第3步】检索知识库（可选）
├─ 条件：启用检索 AND 绑定知识库
├─ 构建检索请求（query, topK, minScore）
├─ 调用 OpenFeign → rag-knowledge
├─ 成功 → 转换为 citations + retrievedContext
└─ 失败 → 打日志，继续执行（降级）
    ↓
【第4步】调用 LLM
├─ 构建 SystemPrompt（含检索上下文）
├─ 调用 chatLanguageModel.generate()
├─ 提取回答文本
├─ 提取/估算 Token 使用量
└─ 失败 → 抛 BizException
    ↓
【第5步】保存 AI 消息
├─ 设置 role = "assistant"
├─ 保存 referencedChunkIds
├─ 保存 retrievalContext
└─ 插入 message 表
    ↓
【第6步】更新会话时间
└─ 更新 lastMessageAt
    ↓
【第7步】构建响应
├─ messageId
├─ answer
├─ citations
├─ tokenInput / tokenOutput
├─ latencyMs
└─ gmtCreate
    ↓
返回 Result<ChatResDTO>
```

---

**文档完成！** 🎉
