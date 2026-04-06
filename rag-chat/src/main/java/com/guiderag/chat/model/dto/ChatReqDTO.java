package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * Chat 请求 DTO
 * 用于接收用户提问内容
 */
@Data
@Schema(description = "对话请求体")
public class ChatReqDTO {
    /**
     * 用户提问内容
     * 必填，最大长度 10000 字符
     */
    @NotBlank(message = "提问内容不能为空")
    @Size(max = 10000, message = "提问内容不能超过10000字符")
    @Schema(description = "用户提问内容", example = "如何配置 Spring Boot 的数据源？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    /**
     * 是否启用知识库检索
     * 默认 true，设为 false 时直接调用 LLM 不检索
     */
    @Schema(description = "是否启用知识库检索", example = "true", defaultValue = "true")
    private Boolean enableRetrieval = true;

    /**
     * 检索返回的最大文档块数量
     * 默认 5，最大 20
     */
    @Schema(description = "检索返回的最大文档块数量", example = "5", defaultValue = "5")
    private Integer topK = 5;

    /**
     * 温度参数（0-1）
     * 控制生成内容的随机性，越低越确定
     */
    @Schema(description = "生成温度参数", example = "0.7", defaultValue = "0.5")
    private Double temperature = 0.5;

    /**
     * 本次对话使用的知识库 ID
     * 如果传入此值，将覆盖会话默认关联的知识库
     */
    @Schema(description = "本次对话关联的知识库ID", example = "2040469400289103872")
    private Long kbId;

    /**
     * 自定义模型配置（可选）
     * 如果传入此值，将使用用户指定的模型而非系统默认模型
     * 支持所有 OpenAI 兼容接口（DeepSeek、OpenRouter、Gemini 等）
     */
    @Schema(description = "自定义模型覆盖配置，传入后使用指定模型替代系统默认模型")
    private ModelOverride modelOverride;

    /**
     * 自定义模型配置 DTO
     * 用于前端动态切换模型，无需重启后端
     */
    @Data
    @Schema(description = "自定义模型配置")
    public static class ModelOverride {

        @Schema(description = "模型提供商标识", example = "openai")
        private String provider;

        @Schema(description = "API Key（敏感信息，仅用于本次请求，不会持久化）", example = "sk-xxx")
        private String apiKey;

        @Schema(description = "API Base URL", example = "https://api.openai.com/v1")
        private String baseUrl;

        @Schema(description = "模型名称", example = "gpt-4o")
        private String model;
    }
}
