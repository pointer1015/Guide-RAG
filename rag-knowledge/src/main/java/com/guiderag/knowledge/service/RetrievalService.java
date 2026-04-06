package com.guiderag.knowledge.service;

import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;

/**
 * 向量检索服务接口
 * 负责：1.查询Embedding → 2.Milvus检索 → 3.关联文档元数据 → 4.返回结果
 */
public interface RetrievalService {

    /**
     * 执行向量检索
     *
     * 核心链路:
     * 用户问题 → 向量化 → Milvus 搜索 → 关联文档 → 返回结果
     *
     * @param tenantId       租户ID（数据隔离）
     * @param knowledgeBaseId 知识库ID
     * @param request        检索请求（query, topK, minScore）
     * @return 检索结果列表
     */
    RetrievalResultDTO retrieve(Long tenantId, Long knowledgeBaseId, RetrievalRequestDTO request);
}
