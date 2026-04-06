package com.guiderag.knowledge.service;


/**
 * 文档处理服务接口
 * 负责文档的异步解析、分块、向量化入库
 */
public interface DocumentProcessService {

    /**
     * 异步处理文档
     * 负责文档的异步解析、分块、向量化入库
     * @param tenantId 租户ID
     * @param documentId 文档Id
     */
    void processDocumentAsync(Long tenantId, Long documentId);


    /**
     * 重新处理失败的文档
     * @param documentId 文档Id
     */
    void retryDocument(Long documentId);
}
