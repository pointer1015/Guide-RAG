package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新会话请求 DTO（仅支持更新标题）
 */
@Data
@Schema(description = "更新会话请求")
public class SessionUpdateReqDTO {

    @Schema(description = "新的会话标题", example = "生产线故障诊断")
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 255, message = "会话标题不能超过255个字符")
    private String title;
}
