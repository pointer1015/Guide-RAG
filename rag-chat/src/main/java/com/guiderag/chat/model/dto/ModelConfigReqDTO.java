package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型配置请求 DTO
 */
@Data
@Schema(description = "用户模型配置请求")
public class ModelConfigReqDTO {

    @Schema(description = "模型提供商标识", example = "openai")
    private String provider;

    @Schema(description = "API Key", example = "sk-xxx")
    private String apiKey;

    @NotBlank(message = "Base URL 不能为空")
    @Schema(description = "API Base URL", example = "https://api.openai.com/v1")
    private String baseUrl;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "gpt-4o")
    private String model;

    @Schema(description = "是否启用（1=启用，0=禁用回退默认模型）", example = "1", defaultValue = "1")
    private Integer isActive = 1;
}
