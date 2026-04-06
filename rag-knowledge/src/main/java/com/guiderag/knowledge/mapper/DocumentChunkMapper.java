package com.guiderag.knowledge.mapper;

import com.guiderag.knowledge.model.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档块 Mapper
 *
 * 核心功能：
 * 1. 批量插入文档块（文档切分后一次性插入所有块）
 * 2. 根据 vectorId 查询（Milvus 检索后关联数据库元数据）
 * 3. 根据文档 ID 查询所有块（用户查看完整文档）
 * 4. 更新 Embedding 状态（异步任务处理进度跟踪）
 * 5. 批量逻辑删除（文档删除时级联删除所有块）
 *
 * @author Guide-RAG Team
 */
@Mapper
public interface DocumentChunkMapper {


    /**
     * 插入单个文档块
     */
    int insert(DocumentChunk chunk);

    /**
     * 批量插入文档块
     *
     * 使用场景：文档解析完成后，将所有切分的块一次性插入数据库
     *
     * 性能优化：
     * - 使用 MyBatis 批量插入（一条 SQL 插入多行）
     * - 比循环单条插入快 10-50 倍
     * - 建议每批不超过 1000 条（避免 SQL 过长）
     *
     * @param chunks 文档块列表
     * @return 插入成功行数
     */
    int batchInsert(@Param("list") List<DocumentChunk> chunks);

    /**
     * 根据向量 ID 批量查询文档块
     *
     * 使用场景：
     * 1. Milvus 向量检索返回 TopK 个 vectorId
     * 2. 通过 vectorId 查询数据库，获取完整元数据（文档标题、文件名、页码等）
     * 3. 合并结果返回给前端展示
     *
     * SQL 性能优化：
     * - vectorId 字段需要建立索引（B-Tree）
     * - 使用 IN 查询比循环单条查询快 5-10 倍
     * - 建议每批不超过 100 个 ID
     *
     * @param vectorIds Milvus 返回的向量 ID 列表
     * @return 文档块列表（包含完整元数据）
     */
    List<DocumentChunk> selectByVectorIds(@Param("list") List<String> vectorIds);

    /**
     * 根据文档 ID 查询所有块（按块索引升序）
     *
     * 使用场景：
     * 1. 用户点击"查看完整文档"
     * 2. 管理后台预览文档分块效果
     * 3. 导出文档内容
     *
     * 排序说明：
     * - 按 chunk_index ASC 排序，保证块的顺序正确
     * - 前端拼接所有块的 contentText 可还原原文
     *
     * @param documentId 文档 ID
     * @return 文档块列表（按顺序排列）
     */
    List<DocumentChunk> selectByDocumentId(Long documentId);

    /**
     * 更新 Embedding 状态
     *
     * 使用场景：
     * 1. 异步任务开始处理：status = 1（处理中）
     * 2. 向量生成成功：status = 2（已完成）
     * 3. 向量生成失败：status = 3（失败）
     *
     * 并发安全：
     * - 使用数据库行锁（SELECT FOR UPDATE）避免重复处理
     * - 状态机保证幂等性：2(已完成) 不会再变更
     *
     * @param id 文档块 ID
     * @param status 新状态（0-未处理, 1-处理中, 2-已完成, 3-失败）
     * @param errorMsg 错误信息（数据库表没有此字段，参数保留用于扩展）
     * @return 更新行数（1-成功, 0-记录不存在）
     */
    int updateEmbeddingStatus(@Param("id") Long id,
                              @Param("status") Integer status,
                              @Param("errorMsg") String errorMsg);

    /**
     * 根据文档 ID 删除所有块
     *
     * 使用场景：
     * 1. 用户删除文档（级联删除所有分块）
     * 2. 文档重新上传（先删除旧块，再插入新块）
     *
     * 注意：
     * - 数据库表没有 is_deleted 字段，使用物理删除
     * - 删除前需先从 Milvus 删除对应向量
     * - 使用事务保证数据一致性
     *
     * @param documentId 文档 ID
     * @return 删除行数
     */
    int logicalDeleteByDocumentId(Long documentId);

    /**
     * 统计文档的块数量
     *
     * 使用场景：
     * 1. 文档列表显示"共 X 个知识块"
     * 2. 校验分块是否完整（应等于文档元数据中的 totalChunks）
     * 3. 监控任务进度：已处理块数 / 总块数
     *
     * @param documentId 文档 ID
     * @return 块数量
     */
    int countByDocumentId(Long documentId);

    /**
     * 查询待处理的文档块（状态 = 0 未处理）
     *
     * 使用场景：
     * 1. 异步任务调度：定时扫描待处理的块
     * 2. 失败重试：扫描 status = 3（失败）的记录
     * @param limit 查询数量上限
     * @return 待处理的文档块列表
     */
    List<DocumentChunk> selectPendingChunks(@Param("limit") int limit);
}
