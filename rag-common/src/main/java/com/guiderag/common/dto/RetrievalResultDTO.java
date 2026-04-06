package com.guiderag.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量检索结果 DTO
 * 
 * 跨服务通用 DTO，供 rag-knowledge 返回，rag-chat 接收
 * 
 * @author Guide-RAG Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量检索结果")
public class RetrievalResultDTO {

    @Schema(description = "检索到的文档块列表")
    private List<ChunkResultDTO> chunks;

    @Schema(description = "结果总数")
    private Integer totalCount;

    @Schema(description = "检索耗时（毫秒）")
    private Long latencyMs;

    /**
     * 返回空结果
     */
    public static RetrievalResultDTO empty() {
        return RetrievalResultDTO.builder()
                .chunks(new ArrayList<>())
                .totalCount(0)
                .latencyMs(0L)
                .build();
    }

    /**
     * 文档块结果 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文档块信息")
    public static class ChunkResultDTO {

        @Schema(description = "文档块唯一标识", example = "1_100_0")
        private String chunkId;

        @Schema(description = "所属文档ID", example = "100")
        private Long documentId;

        @Schema(description = "文档标题", example = "Spring Boot 配置指南")
        private String documentTitle;

        @Schema(description = "文件名", example = "spring-config.pdf")
        private String fileName;

        @Schema(description = "文件类型", example = "pdf")
        private String fileType;

        @Schema(description = "块索引（在文档中的位置）", example = "0")
        private Integer chunkIndex;

        @Schema(description = "文本内容", example = "数据库连接配置示例...")
        private String contentText;

        @Schema(description = "相似度分数（0-1）", example = "0.92")
        private Float score;

        @Schema(description = "元数据（页码、段落号等，JSON格式）")
        private String metadata;
    }
}
