package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 会话响应 DTO
 */
@Data
@Schema(description = "会话信息响应")
public class SessionResDTO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "关联知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "最后消息时间")
    private OffsetDateTime lastMessageAt;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间")
    private LocalDateTime gmtModified;
}
