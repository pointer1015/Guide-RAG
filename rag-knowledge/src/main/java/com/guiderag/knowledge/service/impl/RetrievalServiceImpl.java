package com.guiderag.knowledge.service.impl;

import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;
import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.mapper.DocumentMapper;
import com.guiderag.knowledge.model.entity.Document;
import com.guiderag.knowledge.service.EmbeddingService;
import com.guiderag.knowledge.service.RetrievalService;
import com.guiderag.knowledge.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final DocumentMapper documentMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;    // Embedding 生成服务


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
    @Override
    public RetrievalResultDTO retrieve(Long tenantId, Long knowledgeBaseId, RetrievalRequestDTO request) {

        log.info("[Retrieval] 开始检索: tenantId={}, kbId={}, query={}",
                tenantId, knowledgeBaseId, request.getQuery());

        long start = System.currentTimeMillis();

        // 1 参数校验与默认值设置
        String query = request.getQuery();
        if (query == null || query.trim().isEmpty()) {
            log.warn("[Retrieval] 查询文本为空");
            return RetrievalResultDTO.empty();
        }

        Integer topK = Optional.ofNullable(request.getTopK()).orElse(5);
        Float minScore = Optional.ofNullable(request.getMinScore()).orElse(0.5f);


        // 参数合法性校验
        if (topK <= 0 || topK > 100) {
            throw new BizException("A0400", "topK参数必须在1-100之间");
        }

        if (minScore < 0 || minScore > 1) {
            throw new BizException("A0400", "minScore参数必须在0-1之间");
        }


        try {
            // 2 将查询文本向量化
            log.debug("[Retrieval] Step 1: 生成查询向量...");
            float[] queryVector = embeddingService.embed(query);

            // 检查文本是否为空
            if (queryVector == null || queryVector.length == 0) {
                log.error("[Retrieval] 查询向量生成失败");
                throw new BizException("B0001", "向量生成服务异常");
            }

            log.debug("[Retrieval] 查询向量生成成功，维度: {}", queryVector.length);

            // 3 使用Milvus进行向量相似度搜索
            log.debug("[Retrieval] Step 2: 执行向量搜索, topK={}, minScore={}", topK, minScore);
            List<VectorStoreService.SearchResult> searchResults = 
                    vectorStoreService.search(tenantId, knowledgeBaseId, queryVector, topK, minScore);

            if (searchResults == null || searchResults.isEmpty()) {
                log.info("[Retrieval] 未找到相关内容");
                return RetrievalResultDTO.empty();
            }
            log.debug("[Retrieval] 向量搜索完成，返回 {} 条结果", searchResults.size());

            // 4 关联Document元数据
            log.debug("[Retrieval] Step 3: 关联文档元数据...");

            // 提取所有文档ID（去重）
            Set<Long> documentIds = searchResults.stream()
                    .map(VectorStoreService.SearchResult::getDocumentId)
                    .collect(Collectors.toSet());

            // 批量查询文档信息
            Map<Long, Document> documentMap = new HashMap<>();
            for (Long documentId : documentIds) {
                try {
                    Document doc = documentMapper.selectById(documentId, knowledgeBaseId, tenantId);
                    // 【修复】处理 isDeleted 为 null 的情况，防止自动拆箱导致 NPE
                    if (doc != null && (doc.getIsDeleted() == null || doc.getIsDeleted() == 0)) {
                        documentMap.put(documentId, doc);
                    }
                } catch (Exception e) {
                    log.warn("[Retrieval] 查询文档失败: documentId={}, error={}", documentId, e.getMessage());
                }
            }

            // 5 构建返回结果
            List<RetrievalResultDTO.ChunkResultDTO> chunks = new ArrayList<>();

            for (VectorStoreService.SearchResult searchResult : searchResults) {
                Document doc = documentMap.get(searchResult.getDocumentId());
                // 过滤已删除或者不存在的文档
                if (doc == null) {
                    log.debug("[Retrieval] 跳过已删除或不存在的文档: docId={}", searchResult.getDocumentId());
                    continue;
                }

                // 【优化】标题为空时使用文件名兜底
                String displayTitle = doc.getTitle() != null ? doc.getTitle() : doc.getFileName();

                RetrievalResultDTO.ChunkResultDTO chunk = RetrievalResultDTO.ChunkResultDTO.builder()
                        .chunkId(searchResult.getVectorId())
                        .documentId(searchResult.getDocumentId())
                        .documentTitle(displayTitle)
                        .fileName(doc.getFileName())
                        .fileType(doc.getFileType())
                        .chunkIndex(searchResult.getChunkIndex())
                        .contentText(searchResult.getContentText())
                        .score(searchResult.getScore())
                        .build();
                
                chunks.add(chunk);
            }

            // 按相似度降序排序
            chunks.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

            long duration = System.currentTimeMillis() - start;
            log.info("[Retrieval] 检索完成，耗时 {} ms，返回 {} 条结果", duration, chunks.size());

            return RetrievalResultDTO.builder()
                    .chunks(chunks)
                    .totalCount(chunks.size())
                    .latencyMs(duration)
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Retrieval] 检索异常: {}", e.getMessage(), e);
            throw new BizException("B0001", "向量检索服务异常: " + e.getMessage());
        }
    }




}
