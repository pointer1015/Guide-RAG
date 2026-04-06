package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "消息响应DTO")
public class MessageResDTO {

    @Schema(description = "消息Id")
    private Long messageId;

    @Schema(description = "消息角色")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "内容类型", example = "text")
    private String contentType;

    @Schema(description = "引用的知识块")
    private List<CitationDTO> citations;

    @Schema(description = "消息创建时间")
    private LocalDateTime gmtCreate;

    /**
     * 引用信息内嵌 DTO
     * 用于描述 AI 回复时引用的知识库文档片段
     */
    @Data
    @Schema(description = "引用信息DTO")
    public static class CitationDTO {

        @Schema(description = "文档Id")
        private String docId;

        @Schema(description = "知识块Id")
        private String chunkId;

        @Schema(description = "相关度得分")
        private Double score;

        @Schema(description = "引用原文片段")
        private String sourceText;
    }
}
