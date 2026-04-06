package com.guiderag.chat.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * LLM 模型配置
 * 支持四种内置提供商（通过 llm.provider 配置切换）：
 *   - ollama   : 本地 Ollama 服务（默认，无需 API Key）
 *   - deepseek : DeepSeek 云端 API（国内推荐）
 *   - openai   : OpenAI 云端 API
 *   - gemini   : Gemini(OpenAI 兼容接口)
 *
 * 同时支持自定义 OpenAI 兼容提供商：
 *   - llm.provider=<自定义名称>
 *   - llm.providers.<自定义名称>.api-key / base-url / model
 */
@Slf4j
@Configuration
public class LLMConfig {

    private final Environment environment;

    public LLMConfig(Environment environment) {
        this.environment = environment;
    }

    /* ===================== 通用 ===================== */

    @Value("${llm.provider:ollama}")
    private String provider;

    @Value("${llm.timeout:120}")
    private Integer timeoutSeconds;

    /* ===================== Ollama ===================== */

    @Value("${llm.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${llm.ollama.model:qwen2.5:7b}")
    private String ollamaModel;

    /* ===================== DeepSeek ===================== */

    @Value("${llm.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${llm.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    /* ===================== OpenAI ===================== */

    @Value("${llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${llm.openai.model:gpt-4o}")
    private String openaiModel;

    /* ===================== Gemini(OpenAI 兼容) ===================== */

    @Value("${llm.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${llm.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String geminiBaseUrl;

    @Value("${llm.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // -----------------------------------------------------------------------

    /**
     * 同步聊天模型 Bean
     * 用于：非流式对话、工具调用、知识库检索增强等阻塞式场景
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info(
            "初始化同步 LLM 模型: provider={}, timeout={}s",
            provider,
            timeoutSeconds
        );
        String providerKey = normalizeProvider();
        return switch (providerKey) {
            case "ollama" -> buildOllamaChatModel();
            default -> {
                OpenAiCompatibleConfig config = resolveOpenAiCompatibleConfig(providerKey);
                yield buildOpenAiChatModel(config.apiKey(), config.baseUrl(), config.model());
            }
        };
    }

    /**
     * 流式聊天模型 Bean
     * 用于：SSE 流式输出，前端打字机效果
     *
     * 与同步模型的关键区别：
     *   - ChatLanguageModel.generate()          → 阻塞，等待完整响应
     *   - StreamingChatLanguageModel.generate() → 回调，逐 Token 推送
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info(
            "初始化流式 LLM 模型: provider={}, timeout={}s",
            provider,
            timeoutSeconds
        );
        String providerKey = normalizeProvider();
        return switch (providerKey) {
            case "ollama" -> buildOllamaStreamingChatModel();
            default -> {
                OpenAiCompatibleConfig config = resolveOpenAiCompatibleConfig(providerKey);
                yield buildStreamingOpenAiModel(config.apiKey(), config.baseUrl(), config.model());
            }
        };
    }

    // -----------------------------------------------------------------------
    // Ollama 构建器
    // -----------------------------------------------------------------------

    /**
     * 构建 Ollama 同步模型
     * 前提：本机已启动 Ollama 服务，且已拉取目标模型
     * 拉取示例：ollama pull qwen2.5:7b
     */
    private ChatLanguageModel buildOllamaChatModel() {
        log.info(
            "构建 Ollama 同步模型: baseUrl={}, model={}",
            ollamaBaseUrl,
            ollamaModel
        );
        return OllamaChatModel.builder()
            .baseUrl(ollamaBaseUrl)
            .modelName(ollamaModel)
            .temperature(0.5)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * 构建 Ollama 流式模型
     * Ollama 原生支持流式输出（/api/chat stream=true）
     */
    private StreamingChatLanguageModel buildOllamaStreamingChatModel() {
        log.info(
            "构建 Ollama 流式模型: baseUrl={}, model={}",
            ollamaBaseUrl,
            ollamaModel
        );
        return OllamaStreamingChatModel.builder()
            .baseUrl(ollamaBaseUrl)
            .modelName(ollamaModel)
            .temperature(0.5)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(false) // 流式场景 token 逐条输出，开启日志会导致刷屏
            .logResponses(false)
            .build();
    }

    // -----------------------------------------------------------------------
    // OpenAI 兼容构建器（DeepSeek / OpenAI 共用）
    // -----------------------------------------------------------------------

    /**
     * 构建 OpenAI 兼容同步模型
     * DeepSeek 使用 OpenAI 兼容接口，直接复用此方法
     */
    private ChatLanguageModel buildOpenAiChatModel(
        String apiKey,
        String baseUrl,
        String model
    ) {
        log.info(
            "构建 OpenAI 兼容同步模型: baseUrl={}, model={}",
            baseUrl,
            model
        );
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(0.5)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .maxRetries(2)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * 构建 OpenAI 兼容流式模型
     * 注意：流式模型不支持 maxRetries（流式重试无意义）
     */
    private StreamingChatLanguageModel buildStreamingOpenAiModel(
        String apiKey,
        String baseUrl,
        String model
    ) {
        log.info(
            "构建 OpenAI 兼容流式模型: baseUrl={}, model={}",
            baseUrl,
            model
        );
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(0.5)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(false)
            .logResponses(false)
            .build();
    }

    private String normalizeProvider() {
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase() : "ollama";
    }

    private OpenAiCompatibleConfig resolveOpenAiCompatibleConfig(String providerKey) {
        return switch (providerKey) {
            case "openai" -> new OpenAiCompatibleConfig(openaiApiKey, openaiBaseUrl, openaiModel);
            case "deepseek" -> new OpenAiCompatibleConfig(deepseekApiKey, deepseekBaseUrl, deepseekModel);
            case "gemini" -> new OpenAiCompatibleConfig(geminiApiKey, geminiBaseUrl, geminiModel);
            default -> resolveCustomOpenAiCompatibleConfig(providerKey);
        };
    }

    private OpenAiCompatibleConfig resolveCustomOpenAiCompatibleConfig(String providerKey) {
        String prefix = "llm.providers." + providerKey + ".";
        String apiKey = environment.getProperty(prefix + "api-key", "");
        String baseUrl = environment.getProperty(prefix + "base-url", "");
        String model = environment.getProperty(prefix + "model", "");

        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(model)) {
            throw new IllegalArgumentException(
                "未找到 provider=" + providerKey + " 的配置，请设置 "
                    + prefix + "base-url 与 " + prefix + "model"
            );
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("provider={} 未配置 api-key，将按无密钥模式请求", providerKey);
        }

        log.info("使用自定义 OpenAI 兼容提供商: provider={}, baseUrl={}, model={}",
            providerKey, baseUrl, model);
        return new OpenAiCompatibleConfig(apiKey, baseUrl, model);
    }

    private record OpenAiCompatibleConfig(String apiKey, String baseUrl, String model) {
    }

    // -----------------------------------------------------------------------
    // 公共静态工厂方法（供 ChatServiceImpl 请求级动态构建模型）
    // -----------------------------------------------------------------------

    /**
     * 动态创建同步 ChatLanguageModel
     * 用于前端指定自定义模型时，临时构建模型实例
     *
     * @param provider  提供商（"ollama" 或其他 OpenAI 兼容提供商）
     * @param apiKey    API Key（Ollama 不需要）
     * @param baseUrl   API 地址
     * @param model     模型名称
     * @param timeoutSec 超时秒数
     * @return ChatLanguageModel 实例
     */
    public static ChatLanguageModel createChatModel(
            String provider, String apiKey, String baseUrl, String model, int timeoutSec) {
        log.info("动态创建同步模型: provider={}, baseUrl={}, model={}", provider, baseUrl, model);
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .temperature(0.5)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
        // 其他所有提供商走 OpenAI 兼容接口
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.5)
                .timeout(Duration.ofSeconds(timeoutSec))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 动态创建流式 StreamingChatLanguageModel
     * 用于前端指定自定义模型时，临时构建流式模型实例
     *
     * @param provider  提供商
     * @param apiKey    API Key
     * @param baseUrl   API 地址
     * @param model     模型名称
     * @param timeoutSec 超时秒数
     * @return StreamingChatLanguageModel 实例
     */
    public static StreamingChatLanguageModel createStreamingChatModel(
            String provider, String apiKey, String baseUrl, String model, int timeoutSec) {
        log.info("动态创建流式模型: provider={}, baseUrl={}, model={}", provider, baseUrl, model);
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .temperature(0.5)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .logRequests(false)
                    .logResponses(false)
                    .build();
        }
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.5)
                .timeout(Duration.ofSeconds(timeoutSec))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
