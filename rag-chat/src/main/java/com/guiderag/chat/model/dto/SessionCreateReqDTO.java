package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建会话请求")
public class SessionCreateReqDTO {
    @Schema(description = "会话标题", example = "供应链异常分析")
    @Size(max = 255, message = "会话标题不能超过255个字符")
    private String title;

    @Schema(description = "关联知识库ID（可选）", example = "1234567890123456789")
    private Long knowledgeBaseId;
}
