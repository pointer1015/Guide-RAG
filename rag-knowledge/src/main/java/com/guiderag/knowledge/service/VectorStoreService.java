package com.guiderag.knowledge.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface VectorStoreService {

    // 插入/更新向量数据
    public void upsert(String vectorId, Long tenantId, Long documentId,
                       int chunkIndex, String contentText, float[] embedding);

    // 批量插入向量数据
    public void upsertBatch(List<VectorEntry> entries);


    // 向量相似搜索
    public List<SearchResult> search(Long tenantId, Long knowledgeBaseId,
                                                            float[] queryVector, int topK, float minScore);

    // 删除单条向量
    public void delete(String vectorId);

    // 批量删除向量
    public void deleteBatch(List<String> vectorIds);

    // 删除文档的所有向量
    public void deleteByDocumentId(Long tenantId, Long documentId);

    /**
     * 向量数据条目
     */
    @Data
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public static class VectorEntry {
        private String vectorId;
        private Long tenantId;
        private Long documentId;
        private int chunkIndex;
        private String contentText;
        private float[] embedding;
    }

    /**
     * 搜索结果
     */
    @Data
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public static class SearchResult {
        private String vectorId;
        private Long documentId;
        private int chunkIndex;
        private String contentText;
        private float score;
    }
}
