package com.guiderag.knowledge.service;


import com.guiderag.knowledge.model.entity.DocumentChunk;

import java.util.List;

/**
 * 文本分块服务接口
 *
 * 核心功能：
 * 1. 将长文本切分为多个小块（默认 512 tokens/块）
 * 2. 块之间保持重叠（默认 50 tokens，防止语义断裂）
 * 3. 保留文档结构信息（页码、段落、标题）
 * 4. 计算每块的 Token 数量（用于计费和优化）
 *
 * 推荐配置：
 * - 通用场景：512 tokens/块，50 tokens 重叠
 * - 短问答：256 tokens/块，30 tokens 重叠
 * - 长文档：1024 tokens/块，100 tokens 重叠
 *
 */
public interface ChunkingService {
    /**
     * 将文档文本切分为多个块
     *
     * 处理流程：
     * 1. 文本预处理（清洗、规范化）
     * 2. 按固定大小切分（512 tokens/块）
     * 3. 添加重叠部分（50 tokens）
     * 4. 计算每块的 Token 数量
     * 5. 生成 vectorId（格式：{tenantId}_{documentId}_{chunkIndex}）
     * 6. 封装为 DocumentChunk 对象
     *
     * 边界情况：
     * - 文本为空：返回空列表
     * - 文本很短（< 512 tokens）：返回单个块
     * - 文本超长（> 100万字符）：限制最大块数（默认 1000 块）
     *
     * @param tenantId 租户 ID
     * @param documentId 文档 ID
     * @param content 文档文本内容
     * @return 文档块列表
     */
    List<DocumentChunk> chunkDocument(Long tenantId, Long documentId, String content);
}
