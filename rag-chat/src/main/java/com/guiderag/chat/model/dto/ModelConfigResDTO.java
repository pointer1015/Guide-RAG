package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型配置响应 DTO
 */
@Data
@Schema(description = "用户模型配置响应")
public class ModelConfigResDTO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "模型提供商标识")
    private String provider;

    @Schema(description = "API Key（脱敏后返回）")
    private String apiKey;

    @Schema(description = "API Base URL")
    private String baseUrl;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间")
    private LocalDateTime gmtModified;
}
