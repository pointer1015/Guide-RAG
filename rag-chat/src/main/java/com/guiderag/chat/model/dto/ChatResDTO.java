package com.guiderag.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
/**
 * Chat 响应 DTO
 * 包含 AI 回答内容及引用来源
 */
@Data
@Schema(description = "对话响应体")
public class ChatResDTO {

    /**
     * 消息 ID（AI 回答的消息 ID）
     */
    @Schema(description = "消息ID", example = "1912345678901234567")
    private Long messageId;

    /**
     * AI 回答内容
     */
    @Schema(description = "AI回答内容", example = "根据知识库中的文档，Spring Boot 配置数据源需要...")
    private String answer;

    /**
     * 引用的知识块列表
     * 用于前端展示"来源"信息
     */
    @Schema(description = "引用的知识来源列表")
    private List<CitationDTO> citations;

    /**
     * 输入 Token 数
     */
    @Schema(description = "输入Token数", example = "256")
    private Integer tokenInput;

    /**
     * 输出 Token 数
     */
    @Schema(description = "输出Token数", example = "512")
    private Integer tokenOutput;

    /**
     * 端到端延迟（毫秒）
     */
    @Schema(description = "响应延迟(ms)", example = "1500")
    private Integer latencyMs;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    /**
     * 引用信息内部类
     */
    @Data
    @Schema(description = "引用来源详情")
    public static class CitationDTO {

        @Schema(description = "文档ID", example = "1912345678901234568")
        private String docId;

        @Schema(description = "文档标题", example = "Spring Boot 配置指南.pdf")
        private String docTitle;

        @Schema(description = "分块ID", example = "1912345678901234569")
        private String chunkId;

        @Schema(description = "分块序号", example = "3")
        private Integer chunkIndex;

        @Schema(description = "相关性分数(0-1)", example = "0.92")
        private Double score;

        @Schema(description = "引用的原文片段", example = "数据源配置需要在 application.yml 中添加...")
        private String sourceText;
    }
}
