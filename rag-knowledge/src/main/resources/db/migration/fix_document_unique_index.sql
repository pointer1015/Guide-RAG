-- ============================================================
-- 修复脚本：将 document 表的全局唯一约束改为部分唯一索引
-- 原因：原约束不考虑 is_deleted 字段，导致重新上传已逻辑删除的
--       同内容文档时触发唯一约束冲突（PSQL error 23505）
-- 执行环境：PostgreSQL (guide_rag database)
-- ============================================================

-- 第1步：删除旧的 UNIQUE CONSTRAINT（同时会自动删除底层索引）
-- 注意：不能直接 DROP INDEX，因为索引是由约束管理的，需通过 ALTER TABLE 删除
ALTER TABLE document DROP CONSTRAINT IF EXISTS document_knowledge_base_id_content_hash_key;

-- 第2步：重建为部分唯一索引（Partial Unique Index）
-- 只对未删除（is_deleted = 0）的记录保证 (knowledge_base_id, content_hash) 唯一
-- 已逻辑删除的记录（is_deleted = 1）不参与唯一性约束，允许重复 hash 存在
CREATE UNIQUE INDEX document_knowledge_base_id_content_hash_key
    ON document (knowledge_base_id, content_hash)
    WHERE is_deleted = 0;
