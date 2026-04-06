package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息创建请求 DTO
 * 用于插入用户消息或 AI 回复
 */
@Data
@Schema(description = "消息创建请求")
public class MessageCreateReqDTO {

    @Schema(description = "消息角色", example = "user", allowableValues = {"user", "assistant", "system", "tool"})
    @NotBlank(message = "消息角色不能为空")
    @Pattern(regexp = "^(user|assistant|system|tool)$", message = "消息角色必须是 user/assistant/system/tool 之一")
    private String role;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 32000, message = "消息内容不能超过32000个字符")
    private String content;

    @Schema(description = "内容类型", example = "text", defaultValue = "text")
    private String contentType = "text";

    @Schema(description = "引用的知识块ID列表")
    private String referencedChunkIds;

    @Schema(description = "检索上下文快照")
    private String retrievalContext;

    @Schema(description = "输入Token数")
    private Integer tokenInput;

    @Schema(description = "输出Token数")
    private Integer tokenOutput;

    @Schema(description = "响应延迟（毫秒）")
    private Integer latencyMs;
}
