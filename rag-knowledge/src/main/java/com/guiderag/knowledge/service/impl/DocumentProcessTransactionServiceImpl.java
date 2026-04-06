package com.guiderag.knowledge.service.impl;

import cn.hutool.core.util.IdUtil;
import com.guiderag.common.exception.BizException;
import com.guiderag.common.config.CommonMinioConfig;
import com.guiderag.knowledge.mapper.DocumentChunkMapper;
import com.guiderag.knowledge.mapper.DocumentMapper;
import com.guiderag.knowledge.model.entity.Document;
import com.guiderag.knowledge.model.entity.DocumentChunk;
import com.guiderag.knowledge.service.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理事务服务实现
 * 
 * 职责说明：
 * 1. 所有数据库写操作（PostgreSQL + Milvus）集中在此Service
 * 2. 使用 @Transactional 确保操作的原子性
 * 3. 由 DocumentProcessServiceImpl 通过Spring代理调用，保证事务生效
 * 
 * 事务边界：
 * - PostgreSQL操作：通过Spring事务管理器控制
 * - Milvus操作：虽无法回滚，但通过状态标记保证最终一致性
 * 
 * 异常处理策略：
 * - 任何步骤失败都抛出异常
 * - Spring事务回滚PostgreSQL操作
 * - 文档状态更新为FAILED，记录错误信息
 * - 支持手动重试机制
 * 
 * @author Guide-RAG Team
 * @since 2026-04-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessTransactionServiceImpl implements DocumentProcessTransactionService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final MinioClient minioClient;
    private final CommonMinioConfig minioConfig;
    private final DocumentParserService documentParserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    /**
     * 在事务中执行文档处理
     * 
     * 修复说明：
     * - 原代码在 DocumentProcessServiceImpl 的同类方法调用中使用 @Transactional
     * - 由于Spring AOP限制，同类内部调用无法触发事务代理
     * - 现在拆分为独立Service，通过Spring容器调用确保事务生效
     * 
     * @param doc 待处理文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDocumentInTransaction(Document doc) {
        Long documentId = doc.getId();

        try {
            // ==================== Step 1: 更新状态为 PARSING ====================
            documentMapper.updateParseStatus(documentId, "PARSING", null);
            log.info("[DocumentProcess] ✓ Step 1: 状态更新为 PARSING, docId={}", documentId);

            // ==================== Step 2-3: 下载并解析文档 ====================
            log.info("[DocumentProcess] Step 2: 从 MinIO 下载文件: bucket={}, key={}",
                    doc.getMinioBucket(), doc.getMinioObjectKey());

            String textContent;
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(doc.getMinioBucket())
                            .object(doc.getMinioObjectKey())
                            .build()
            )) {
                log.info("[DocumentProcess] Step 3: 使用 Apache Tika 解析文档");
                textContent = documentParserService.parseDocument(inputStream, doc.getFileName());
            }

            log.info("[DocumentProcess] ✓ Step 2-3: 文档解析完成，内容长度: {} 字符", textContent.length());

            // ==================== Step 4: 文本分块 ====================
            log.info("[DocumentProcess] Step 4: 使用 LangChain4j 执行文本分块");
            List<DocumentChunk> chunks = chunkingService.chunkDocument(
                    doc.getTenantId(), documentId, textContent);

            if (chunks.isEmpty()) {
                log.warn("[DocumentProcess] ⚠ 分块结果为空，文档内容可能过短");
            }
            log.info("[DocumentProcess] ✓ Step 4: 分块完成，共生成 {} 个块", chunks.size());

            // ==================== Step 5: 批量生成Embedding ====================
            log.info("[DocumentProcess] Step 5: 批量生成 Embedding 向量");
            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getContentText)
                    .toList();

            List<float[]> embeddings = embeddingService.embedBatch(texts);

            if (embeddings.size() != chunks.size()) {
                throw new BizException("B0001", 
                        String.format("向量数量与分块数量不匹配: %d vs %d", embeddings.size(), chunks.size()));
            }
            log.info("[DocumentProcess] ✓ Step 5: Embedding生成完成，共 {} 个向量", embeddings.size());

            // ==================== Step 6: 保存分块元数据到PostgreSQL ====================
            log.info("[DocumentProcess] Step 6: 保存分块元数据到 PostgreSQL");
            for (DocumentChunk chunk : chunks) {
                chunk.setId(IdUtil.getSnowflakeNextId());
                documentChunkMapper.insert(chunk);
            }
            log.info("[DocumentProcess] ✓ Step 6: 元数据已保存，共 {} 条记录", chunks.size());

            // ==================== Step 7: 批量写入向量到Milvus ====================
            log.info("[DocumentProcess] Step 7: 批量写入向量数据到 Milvus");
            List<VectorStoreService.VectorEntry> vectorEntries = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);

                VectorStoreService.VectorEntry entry = VectorStoreService.VectorEntry.builder()
                        .vectorId(chunk.getVectorId())
                        .tenantId(chunk.getTenantId())
                        .documentId(chunk.getDocumentId())
                        .chunkIndex(chunk.getChunkIndex())
                        .contentText(chunk.getContentText())
                        .embedding(embedding)
                        .build();
                vectorEntries.add(entry);
            }
            vectorStoreService.upsertBatch(vectorEntries);
            log.info("[DocumentProcess] ✓ Step 7: 向量数据写入 Milvus 成功");

            // ==================== Step 8: 更新状态为 PARSED ====================
            documentMapper.updateParseStatusSuccess(documentId, "PARSED",
                    chunks.size(), LocalDateTime.now());

            log.info("[DocumentProcess] ✅ 文档处理完成: docId={}, totalChunks={}, status=PARSED",
                    documentId, chunks.size());

        } catch (Exception e) {
            log.error("[DocumentProcess] ❌ 文档处理失败: docId={}, error={}",
                    documentId, e.getMessage(), e);

            // 更新状态为 FAILED，记录错误信息，增加重试计数
            try {
                documentMapper.updateParseStatusFailed(documentId, "FAILED",
                        e.getMessage(), doc.getRetryCount() + 1);
            } catch (Exception updateEx) {
                log.error("[DocumentProcess] 更新失败状态时发生异常: {}", updateEx.getMessage());
            }

            // 抛出异常触发事务回滚
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }
}
