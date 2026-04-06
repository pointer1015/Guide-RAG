package com.guiderag.knowledge.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;


/**
 * Embedding 模型配置类
 * <p>
 * 支持多种嵌入模型后端:
 * - OpenAI: text-embedding-3-small/large, text-embedding-ada-002
 * - Ollama: nomic-embed-text, bge-m3（本地部署）
 * - Gemini: 通过 OpenAI 兼容接口
 * - 自定义 OpenAI 兼容提供商: embedding.providers.<provider>.*
 * <p>
 * 配置示例 (application.yml):
 * embedding:
 * provider: openai  # openai / ollama
 * openai:
 * api-key: ${OPENAI_API_KEY}
 * model: text-embedding-3-small
 * ollama:
 * base-url: http://localhost:11434
 * model: nomic-embed-text
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    private final Environment environment;

    public EmbeddingConfig(Environment environment) {
        this.environment = environment;
    }

    // 嵌入模型提供商: openai / ollama
    private String provider = "openai";

    // OpenAI 配置
    private OpenAIConfig openai = new OpenAIConfig();

    // Ollama本地配置
    private OllamaConfig ollama = new OllamaConfig();

    // Gemini(OpenAI兼容)配置
    private OpenAIConfig gemini = new OpenAIConfig();

    @Data
    public static class OpenAIConfig {
        // API-Key
        private String apiKey;

        // 基础url
        private String baseUrl = "https://api.openai.com/v1";

        // 模型名称
        private String model = "text-embedding-3-small";

        // 请求超时时间，单位毫秒
        private int timeout = 10000;

        // 向量维度
        private Integer dimensions;
    }

    @Data
    public static class OllamaConfig {
        // Ollama API 基础URL
        private String baseUrl = "http://localhost:11434";

        // 模型名称
        private String model = "nomic-embed-text";

        // 请求超时时间，单位秒
        private int timeout = 120;
    }

    /**
     * 创建 EmbeddingModel Bean
     * <p>
     * 根据 provider 配置自动选择对应的模型实现
     *
     * @return LangChain4j EmbeddingModel 实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        String providerKey = normalizeProvider();
        log.info("[EmbeddingConfig] 初始化嵌入模型, provider: {}", providerKey);

        return switch (providerKey) {
            case "openai" -> createOpenAiEmbeddingModel();
            case "ollama" -> createOllamaEmbeddingModel();
            case "gemini" -> createOpenAiCompatibleEmbeddingModel(gemini);
            default -> createCustomOpenAiCompatibleEmbeddingModel(providerKey);
        };
    }

    /**
     * 创建 OpenAI 兼容的嵌入模型
     * <p>
     * 同时支持:
     * - OpenAI 官方 API
     * - DeepSeek Embedding API
     * - Azure OpenAI
     * - 其他兼容接口
     */
    private EmbeddingModel createOpenAiEmbeddingModel() {
        return createOpenAiCompatibleEmbeddingModel(openai);
    }

    private EmbeddingModel createOpenAiCompatibleEmbeddingModel(OpenAIConfig config) {
        log.info("[EmbeddingConfig] 创建 OpenAI Embedding: model={}, baseUrl={}",
                config.getModel(), config.getBaseUrl());

        var builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModel())
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .logRequests(true)
                .logResponses(true);

        // 如果配置了自定义维度（text-embedding-3-* 支持）
        if (config.getDimensions() != null) {
            builder.dimensions(config.getDimensions());
        }

        return builder.build();
    }

    /**
     * 创建 Ollama 本地嵌入模型
     * <p>
     * 推荐模型:
     * - nomic-embed-text: 768 维，通用性好
     * - bge-m3: 1024 维，中文优化
     * - mxbai-embed-large: 1024 维，高性能
     */
    private EmbeddingModel createOllamaEmbeddingModel() {
        log.info("[EmbeddingConfig] 创建 Ollama Embedding: model={}, baseUrl={}",
                ollama.getModel(), ollama.getBaseUrl());

        return OllamaEmbeddingModel.builder()
                .baseUrl(ollama.getBaseUrl())
                .modelName(ollama.getModel())
                .timeout(Duration.ofSeconds(ollama.getTimeout()))
                .build();
    }

    public String resolveCurrentModelName() {
        String providerKey = normalizeProvider();
        return switch (providerKey) {
            case "openai" -> openai.getModel();
            case "ollama" -> ollama.getModel();
            case "gemini" -> gemini.getModel();
            default -> environment.getProperty("embedding.providers." + providerKey + ".model", "unknown");
        };
    }

    private EmbeddingModel createCustomOpenAiCompatibleEmbeddingModel(String providerKey) {
        String prefix = "embedding.providers." + providerKey + ".";
        String apiKey = environment.getProperty(prefix + "api-key", "");
        String baseUrl = environment.getProperty(prefix + "base-url", "");
        String model = environment.getProperty(prefix + "model", "");
        Integer dimensions = environment.getProperty(prefix + "dimensions", Integer.class);
        int timeout = environment.getProperty(prefix + "timeout", Integer.class, 60);

        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(model)) {
            throw new IllegalArgumentException(
                    "未找到 embedding provider=" + providerKey + " 的配置，请设置 "
                            + prefix + "base-url 与 " + prefix + "model");
        }

        OpenAIConfig custom = new OpenAIConfig();
        custom.setApiKey(apiKey);
        custom.setBaseUrl(baseUrl);
        custom.setModel(model);
        custom.setTimeout(timeout);
        custom.setDimensions(dimensions);
        log.info("[EmbeddingConfig] 使用自定义 OpenAI 兼容 Embedding 提供商: provider={}, baseUrl={}, model={}",
                providerKey, baseUrl, model);
        return createOpenAiCompatibleEmbeddingModel(custom);
    }

    private String normalizeProvider() {
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "openai";
    }

}
