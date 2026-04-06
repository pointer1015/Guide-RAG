package com.guiderag.knowledge.service;

import com.guiderag.knowledge.model.entity.Document;

/**
 * 文档处理事务服务
 * 
 * 设计目的：
 * 将事务性操作拆分到独立Service，避免同类内部调用导致事务失效
 * 
 * 背景：
 * - 原代码在 DocumentProcessServiceImpl 的 @Async 方法中调用同类的 @Transactional 方法
 * - Spring AOP 无法拦截同类内部调用，导致事务注解不生效
 * 
 * 解决方案：
 * - 创建独立的 TransactionService，通过 Spring 代理调用确保事务生效
 * - DocumentProcessServiceImpl 负责异步调度
 * - DocumentProcessTransactionService 负责事务性数据处理
 * 
 * @author Guide-RAG Team
 * @since 2026-04-03
 */
public interface DocumentProcessTransactionService {
    
    /**
     * 在事务中执行文档处理
     * 
     * 完整流程：
     * 1. 更新文档状态为 PARSING
     * 2. 从MinIO下载文件并使用Tika解析
     * 3. 使用LangChain4j进行文本分块
     * 4. 批量生成Embedding向量
     * 5. 保存分块元数据到PostgreSQL
     * 6. 批量写入向量到Milvus
     * 7. 更新文档状态为 PARSED
     * 
     * 若任一步骤失败，整个事务回滚，状态更新为 FAILED
     * 
     * @param doc 待处理的文档实体
     * @throws RuntimeException 处理失败时抛出异常触发事务回滚
     */
    void processDocumentInTransaction(Document doc);
}
