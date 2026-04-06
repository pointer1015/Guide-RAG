package com.guiderag.knowledge.service.impl;

import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.mapper.DocumentMapper;
import com.guiderag.knowledge.model.entity.Document;
import com.guiderag.knowledge.service.DocumentProcessService;
import com.guiderag.knowledge.service.DocumentProcessTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文档处理服务实现（重构版）
 * 
 * 重构说明：
 * 1. 原代码问题：@Async方法内部调用同类的@Transactional方法，事务不生效
 * 2. 重构方案：将事务逻辑拆分到独立的 DocumentProcessTransactionService
 * 3. 本Service职责简化为：异步调度 + 状态校验 + 重试控制
 * 
 * 职责分工：
 * - DocumentProcessServiceImpl: 异步调度器，负责任务分发和状态检查
 * - DocumentProcessTransactionService: 事务处理器，负责实际的文档处理逻辑
 * 
 * 优势：
 * - 清晰的职责分离，符合单一职责原则
 * - 通过Spring代理调用确保事务生效
 * - 易于单元测试和维护
 * 
 * @author Guide-RAG Team
 * @since 2026-04-03 (重构)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessServiceImpl implements DocumentProcessService {

    private final DocumentMapper documentMapper;
    
    /**
     * 【关键修复】注入事务Service
     * 通过Spring容器管理的代理对象调用，确保@Transactional生效
     */
    private final DocumentProcessTransactionService transactionService;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 异步处理文档
     * 
     * 使用@Async注解，任务会在独立线程池中执行，不阻塞主线程
     * 线程池配置在 AsyncConfig 中定义
     * 
     * @param tenantId 租户ID
     * @param documentId 文档ID
     */
    @Async("documentProcessExecutor")
    @Override
    public void processDocumentAsync(Long tenantId, Long documentId) {
        log.info("[DocumentProcess] 📥 开始异步处理文档: tenantId={}, documentId={}", tenantId, documentId);

        // 设置当前线程的用户上下文
        com.guiderag.common.context.UserContextHolder.setUserId(tenantId);

        try {
            // 查询文档
            Document doc = documentMapper.selectByIdOnly(documentId);
            if (doc == null) {
                log.error("[DocumentProcess] ❌ 文档不存在: documentId={}", documentId);
                return;
            }

            // 状态校验：只处理 PENDING 或 FAILED（未超过重试次数）的文档
            String status = doc.getParseStatus();
            if (!"PENDING".equals(status) && !"FAILED".equals(status)) {
                log.warn("[DocumentProcess] ⚠ 跳过处理，状态不符: documentId={}, status={}",
                        documentId, status);
                return;
            }

            // 检查重试次数
            if ("FAILED".equals(status) && doc.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("[DocumentProcess] ⚠ 已达最大重试次数，跳过: documentId={}, retryCount={}",
                        documentId, doc.getRetryCount());
                return;
            }

            // 【关键】通过Spring代理调用事务方法
            try {
                transactionService.processDocumentInTransaction(doc);
            } catch (Exception e) {
                // 异常已在transactionService中处理（状态已更新为FAILED）
                log.error("[DocumentProcess] ❌ 文档处理失败（已记录）: documentId={}", documentId, e);
            }
        } finally {
            // 清除上下文，防止线程污染
            com.guiderag.common.context.UserContextHolder.clear();
        }
    }

    @Override
    public void retryDocument(Long documentId) {
        log.info("[DocumentProcess] 🔄 手动重试文档: documentId={}", documentId);

        Document doc = documentMapper.selectByIdOnly(documentId);
        if (doc == null) {
            throw new BizException("A0500", "文档不存在");
        }

        if (!"FAILED".equals(doc.getParseStatus())) {
            throw new BizException("A0400", "只能重试失败状态的文档，当前状态: " + doc.getParseStatus());
        }

        if (doc.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new BizException("A0400", 
                    String.format("已达最大重试次数(%d)，请检查文档后重新上传", MAX_RETRY_COUNT));
        }

        // 获取租户ID并触发异步处理
        processDocumentAsync(doc.getTenantId(), documentId);
    }
}
