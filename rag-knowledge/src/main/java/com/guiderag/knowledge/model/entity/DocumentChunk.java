package com.guiderag.knowledge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档块实体
 * <p>
 * 对应数据库表：document_chunk
 * 存储文档切分后的文本块及其向量化元数据
 * <p>
 * 核心字段说明：
 * - vectorId: Milvus 向量 ID，格式："tenantId_documentId_chunkIndex"（如 "1_100_0"）
 * - contentText: 文本块内容，用于 Embedding 生成向量
 * - chunkIndex: 块在文档中的顺序，从 0 开始
 * - tokenCount: 文本块的 Token 数量（用于计费和分块策略）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 主键ID（数据库自增）
     */
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 所属文档ID（外键关联 document 表）
     */
    private Long documentId;

    /**
     * 块索引（在文档中的位置）
     */
    private Integer chunkIndex;

    /**
     * 内容模态（类型）
     */
    private String modality;

    /**
     * 文本块内容
     */
    private String contentText;

    /**
     * Token 数量
     */
    private Integer tokenCount;

    /**
     * Embedding 模型名称
     */
    private String embeddingModel;

    /**
     * Milvus 向量 ID
     */
    private String vectorId;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 创建时间
     * 对应数据库字段：gmt_create
     */
    private LocalDateTime gmtCreate;

    /**
     * 向量化状态
     * 0: 未向量化
     * 1: 向量化成功
     * 2: 向量化失败
     */
    private Integer embeddingStatus;

    // 0-正常, 1-已删除
    private Integer isDeleted;

}
