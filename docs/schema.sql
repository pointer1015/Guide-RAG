-- 基础系统结构：用于工业级多模态 RAG 系统的数据库初始化脚本
-- 遵循优化建议：
-- 1. 取消 BIGSERIAL，采用 BIGINT（配合 MyBatis-Plus 雪花 ID）
-- 2. 取消 ENUM，采用 VARCHAR（降低维护成本）
-- 3. 增加乐观锁 version 和重试次数 retry_count
-- 4. 优化唯一约束范围

-- ==========================================
-- 1. 租户表 (tenant)
-- ==========================================
CREATE TABLE tenant (
    id            BIGINT PRIMARY KEY,
    tenant_code   VARCHAR(64) NOT NULL UNIQUE,
    tenant_name   VARCHAR(255) NOT NULL,
    plan          VARCHAR(30) NOT NULL DEFAULT 'free',
    gmt_create    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE tenant IS '租户信息表';
COMMENT ON COLUMN tenant.id IS '租户ID';
COMMENT ON COLUMN tenant.tenant_code IS '租户唯一编码';
COMMENT ON COLUMN tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN tenant.plan IS '订阅计划(free/pro/enterprise)';
COMMENT ON COLUMN tenant.gmt_create IS '创建时间';
COMMENT ON COLUMN tenant.gmt_modified IS '更新时间';

-- ==========================================
-- 2. 用户表 (user)
-- ==========================================
CREATE TABLE "user" (
    id            BIGINT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'active',
    is_deleted    SMALLINT NOT NULL DEFAULT 0,
    gmt_create    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE "user" IS '全局用户表';
COMMENT ON COLUMN "user".id IS '用户ID';
COMMENT ON COLUMN "user".email IS '登录邮箱';
COMMENT ON COLUMN "user".password_hash IS '加密后的哈希密码';
COMMENT ON COLUMN "user".display_name IS '用户显示昵称';
COMMENT ON COLUMN "user".status IS '用户状态(active/locked)';
COMMENT ON COLUMN "user".is_deleted IS '软删除标志(0:正常, 1:已删除)';
COMMENT ON COLUMN "user".gmt_create IS '创建时间';
COMMENT ON COLUMN "user".gmt_modified IS '更新时间';

-- ==========================================
-- 3. 租户用户关联表 (tenant_user)
-- ==========================================
CREATE TABLE tenant_user (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    role          VARCHAR(20) NOT NULL DEFAULT 'member',
    gmt_create    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, user_id)
);
COMMENT ON TABLE tenant_user IS '租户与用户的多对多关联表';
COMMENT ON COLUMN tenant_user.id IS '关联主键ID';
COMMENT ON COLUMN tenant_user.tenant_id IS '租户ID';
COMMENT ON COLUMN tenant_user.user_id IS '用户ID';
COMMENT ON COLUMN tenant_user.role IS '租户内角色(owner/admin/member)';
COMMENT ON COLUMN tenant_user.gmt_create IS '加入时间';

-- ==========================================
-- 4. 知识库表 (knowledge_base)
-- ==========================================
CREATE TABLE knowledge_base (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    created_by    BIGINT NOT NULL,
    version       INT NOT NULL DEFAULT 0,
    is_deleted    SMALLINT NOT NULL DEFAULT 0,
    gmt_create    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, name)
);
COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.id IS '知识库ID';
COMMENT ON COLUMN knowledge_base.tenant_id IS '归属租户ID';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '知识库详细描述';
COMMENT ON COLUMN knowledge_base.created_by IS '创建人(用户ID)';
COMMENT ON COLUMN knowledge_base.version IS '乐观锁版本号';
COMMENT ON COLUMN knowledge_base.is_deleted IS '软删除标志(0:正常, 1:已删除)';
COMMENT ON COLUMN knowledge_base.gmt_create IS '创建时间';
COMMENT ON COLUMN knowledge_base.gmt_modified IS '最后修改时间';

-- ==========================================
-- 5. 文档表 (document)
-- ==========================================
CREATE TABLE document (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    uploaded_by      BIGINT NOT NULL,
    title            VARCHAR(255),
    file_name        VARCHAR(255) NOT NULL,
    file_type        VARCHAR(20) NOT NULL DEFAULT 'other',
    file_size        BIGINT NOT NULL DEFAULT 0,
    mime_type        VARCHAR(120),
    minio_bucket     VARCHAR(100) NOT NULL,
    minio_object_key VARCHAR(500) NOT NULL,
    content_hash     CHAR(64),
    parse_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parse_error      TEXT,
    retry_count      INT NOT NULL DEFAULT 0,
    parsed_at        TIMESTAMP WITH TIME ZONE,
    version          INT NOT NULL DEFAULT 0,
    gmt_create       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (knowledge_base_id, content_hash)
);
COMMENT ON TABLE document IS '知识库文档表';
COMMENT ON COLUMN document.id IS '文档ID';
COMMENT ON COLUMN document.tenant_id IS '归属租户ID';
COMMENT ON COLUMN document.knowledge_base_id IS '归属知识库ID';
COMMENT ON COLUMN document.uploaded_by IS '上传人ID';
COMMENT ON COLUMN document.title IS '文档标题(可选)';
COMMENT ON COLUMN document.file_name IS '原始文件名称';
COMMENT ON COLUMN document.file_type IS '文件大类(pdf/image/audio/other)';
COMMENT ON COLUMN document.file_size IS '文件大小(字节)';
COMMENT ON COLUMN document.mime_type IS '文件MIME类型';
COMMENT ON COLUMN document.minio_bucket IS 'MinIO存储桶名';
COMMENT ON COLUMN document.minio_object_key IS 'MinIO对象全路径';
COMMENT ON COLUMN document.content_hash IS '文件内容的SHA256哈希值，用于去重';
COMMENT ON COLUMN document.parse_status IS '解析状态(PENDING/PARSING/PARSED/FAILED)';
COMMENT ON COLUMN document.parse_error IS '解析失败原因堆栈记录';
COMMENT ON COLUMN document.retry_count IS '解析失败重试次数';
COMMENT ON COLUMN document.parsed_at IS '解析完成时间';
COMMENT ON COLUMN document.version IS '乐观锁版本号';
COMMENT ON COLUMN document.gmt_create IS '创建时间';
COMMENT ON COLUMN document.gmt_modified IS '最后修改时间';
COMMENT ON COLUMN document.is_deleted IS '软删除标志(0:正常, 1:已删除)';

CREATE INDEX idx_document_tenant_id ON document(tenant_id);
CREATE INDEX idx_document_kb_id ON document(knowledge_base_id);
CREATE INDEX idx_document_uploaded_by ON document(uploaded_by);
CREATE INDEX idx_document_parse_status ON document(parse_status);
CREATE INDEX idx_document_gmt_create ON document(gmt_create DESC);
CREATE INDEX idx_document_bucket_key ON document(minio_bucket, minio_object_key);

-- ==========================================
-- 6. 文档分块表 (document_chunk)
-- ==========================================
CREATE TABLE document_chunk (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    document_id     BIGINT NOT NULL,
    chunk_index     INT NOT NULL,
    modality        VARCHAR(30) NOT NULL DEFAULT 'text',
    content_text    TEXT,
    token_count     INT NOT NULL DEFAULT 0,
    embedding_model VARCHAR(100) NOT NULL,
    vector_id       VARCHAR(128) NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    gmt_create      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, chunk_index)
);
COMMENT ON TABLE document_chunk IS '文档文本分块及向量映射表';
COMMENT ON COLUMN document_chunk.id IS '分块ID';
COMMENT ON COLUMN document_chunk.tenant_id IS '归属租户ID';
COMMENT ON COLUMN document_chunk.document_id IS '归属文档ID';
COMMENT ON COLUMN document_chunk.chunk_index IS '分块在该文档中的顺序索引';
COMMENT ON COLUMN document_chunk.modality IS '模态类型(text/image/audio_transcript)';
COMMENT ON COLUMN document_chunk.content_text IS '分块的实际文本内容';
COMMENT ON COLUMN document_chunk.token_count IS '大模型计算的Token数量';
COMMENT ON COLUMN document_chunk.embedding_model IS '所采用的Embedding模型名称';
COMMENT ON COLUMN document_chunk.vector_id IS '向量数据库(如Milvus/Qdrant)中的对应向量ID';
COMMENT ON COLUMN document_chunk.metadata IS '附加元数据(如页码、坐标等)';
COMMENT ON COLUMN document_chunk.gmt_create IS '分块生成时间';

CREATE INDEX idx_chunk_tenant_doc ON document_chunk(tenant_id, document_id);
CREATE INDEX idx_chunk_vector_id ON document_chunk(vector_id);
CREATE INDEX gin_chunk_metadata ON document_chunk USING GIN(metadata);

-- ==========================================
-- 7. 会话表 (session)
-- ==========================================
CREATE TABLE session (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    user_id          BIGINT NOT NULL,
    knowledge_base_id BIGINT,
    title            VARCHAR(255),
    last_message_at  TIMESTAMP WITH TIME ZONE,
    gmt_create       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE session IS '用户问答会话表';
COMMENT ON COLUMN session.id IS '会话ID';
COMMENT ON COLUMN session.tenant_id IS '归属租户ID';
COMMENT ON COLUMN session.user_id IS '提问用户ID';
COMMENT ON COLUMN session.knowledge_base_id IS '会话限定的专属知识库ID(可为空代表全租户检索)';
COMMENT ON COLUMN session.title IS '会话标题(通常由大模型自动生成)';
COMMENT ON COLUMN session.last_message_at IS '最后一条消息的发送时间';
COMMENT ON COLUMN session.gmt_create IS '会话创建时间';
COMMENT ON COLUMN session.gmt_modified IS '会话更新时间';
COMMENT ON COLUMN session.is_deleted IS '软删除标志(0:正常, 1:已删除)';

CREATE INDEX idx_session_tenant_user ON session(tenant_id, user_id);
CREATE INDEX idx_session_last_message_at ON session(last_message_at DESC);

-- ==========================================
-- 8. 消息表 (message)
-- ==========================================
CREATE TABLE message (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL,
    session_id           BIGINT NOT NULL,
    user_id              BIGINT,
    role                 VARCHAR(20) NOT NULL,
    content              TEXT NOT NULL,
    content_type         VARCHAR(30) NOT NULL DEFAULT 'text',
    referenced_chunk_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    retrieval_context    JSONB NOT NULL DEFAULT '{}'::jsonb,
    token_input          INT NOT NULL DEFAULT 0,
    token_output         INT NOT NULL DEFAULT 0,
    latency_ms           INT NOT NULL DEFAULT 0,
    gmt_create           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted           SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE message IS '会话消息记录表';
COMMENT ON COLUMN message.id IS '消息ID';
COMMENT ON COLUMN message.tenant_id IS '归属租户ID';
COMMENT ON COLUMN message.session_id IS '归属会话ID';
COMMENT ON COLUMN message.user_id IS '发送者ID(若是大模型回复则可能视设计为空或对应马甲ID)';
COMMENT ON COLUMN message.role IS '消息角色(user/assistant/system/tool)';
COMMENT ON COLUMN message.content IS '消息正文';
COMMENT ON COLUMN message.content_type IS '消息格式(text/image_url等)';
COMMENT ON COLUMN message.referenced_chunk_ids IS 'AI回复时引用的知识块ID集合，用于前端溯源展现';
COMMENT ON COLUMN message.retrieval_context IS '检索命中的上下文快照，用于离线评估';
COMMENT ON COLUMN message.token_input IS '本次提问消耗的输入Token数';
COMMENT ON COLUMN message.token_output IS '本次回答消耗的输出Token数';
COMMENT ON COLUMN message.latency_ms IS '大模型响应或流式首字耗时(毫秒)';
COMMENT ON COLUMN message.gmt_create IS '消息记录时间';
COMMENT ON COLUMN message.is_deleted IS '软删除标志(0:正常, 1:已删除)';

CREATE INDEX idx_message_tenant_id ON message(tenant_id);
CREATE INDEX idx_message_session_id_gmt_create ON message(session_id, gmt_create);
CREATE INDEX idx_message_gmt_create ON message(gmt_create DESC);
CREATE INDEX gin_message_referenced_chunk_ids ON message USING GIN(referenced_chunk_ids);
