# Auth 模块完成后的下一步开发路线图

## 📊 项目现状总览

### 已完成模块
| 模块 | 状态 | 关键能力 |
|------|------|----------|
| `rag-auth` | ✅ 已完成 | 注册、登录（验证码）、登出、Token 刷新、用户信息查询 |
| `rag-gateway` | ✅ 已完成 | JWT 全局过滤器、白名单放行、`X-User-Id` 透传 |
| `rag-common` | ✅ 已完成 | `JwtUtils`、统一 `Result` 响应、全局异常处理、`AuthConstants` |

### 骨架模块（仅有启动类）
| 模块 | 状态 | 待实现 |
|------|------|--------|
| `rag-knowledge` | 🔲 骨架 | 知识库 CRUD、文档上传/解析/向量化 |
| `rag-chat` | 🔲 骨架 | 会话管理、消息管理、RAG 问答、SSE 流式输出 |

### 基础设施
- **数据库 DDL**：`schema.sql` 已定义 8 张表（`tenant` → `message`）
- **API 文档**：6 大模块接口规范已定义完整
- **Knife4j**：已集成 Swagger/OpenAPI 文档

---

## 🗺️ 作为资深架构师的下一步行动路线

### 第一阶段：知识库管理模块 `rag-knowledge`（建议优先级 P0）

> [!IMPORTANT]
> 知识库是 RAG 系统的**数据基座**，文档上传-解析-向量化链路是最核心的业务闭环。建议作为紧随 auth 之后的首要开发目标。

#### 1.1 知识库 CRUD（核心基础）
- `POST /rag/v1/knowledge-bases` — 创建知识库
- `GET /rag/v1/knowledge-bases` — 分页查询知识库列表
- `GET /rag/v1/knowledge-bases/{id}` — 获取知识库详情
- `PUT /rag/v1/knowledge-bases/{id}` — 更新知识库
- `DELETE /rag/v1/knowledge-bases/{id}` — 删除知识库（非空校验）

#### 1.2 文档管理（上传与元数据）
- `POST /rag/v1/knowledge-bases/{kbId}/documents` — 文件上传（MinIO 存储 + 元数据入库）
- `GET /rag/v1/knowledge-bases/{kbId}/documents` — 分页查询文档列表
- `GET /rag/v1/documents/{id}` — 获取文档详情（含解析状态）
- `DELETE /rag/v1/documents/{id}` — 删除文档（软删除 + MinIO 清理）

#### 1.3 文档解析与向量化（异步链路）
- 文档解析（Apache Tika / PDFBox）
- 文本切片（按 Token 窗口切分）
- Embedding 生成（BGE-M3 via LangChain4j）
- 向量入库（Milvus/Qdrant）
- 状态流转：`PENDING → PARSING → PARSED/FAILED`

---

### 第二阶段：会话与问答模块 `rag-chat`（建议优先级 P0）

#### 2.1 会话管理
- `POST /rag/v1/sessions` — 创建会话
- `GET /rag/v1/sessions` — 会话列表（分页）
- `GET /rag/v1/sessions/{id}` — 会话详情
- `PATCH /rag/v1/sessions/{id}` — 更新会话标题
- `DELETE /rag/v1/sessions/{id}` — 删除会话

#### 2.2 消息与 RAG 问答
- `GET /rag/v1/sessions/{id}/messages` — 获取历史消息
- `POST /rag/v1/chat/completions` — 同步问答
- `POST /rag/v1/chat/stream` — **SSE 流式问答**（核心卖点）
  - 向量检索 → Reranker → Prompt 构建 → LLM 生成 → SSE 推送

---

### 第三阶段：基础设施完善（建议优先级 P1）

#### 3.1 MinIO 对象存储集成
- 封装 `MinioService`（上传、下载、预签名 URL、删除）
- Bucket 初始化策略

#### 3.2 向量数据库集成
- 封装 `VectorStoreService`（upsert、search、delete）
- Collection 创建与索引维护

#### 3.3 Redis 增强
- 会话上下文缓存
- 限流器（令牌桶）
- 分布式锁（文档解析防并发）

---

### 第四阶段：前端开发（建议优先级 P1）

- Vue 3 + TypeScript + Vite 6
- 登录/注册页面（对接验证码 + JWT）
- 知识库管理界面
- 聊天问答界面（SSE 流式渲染 + Markdown）

---

## 🔍 我作为架构师，当前会**立即做的事情**

作为一位资深架构师，在 auth 模块完成交付后，我会按照以下优先级推进：

### ① 立即启动 `rag-knowledge` 模块开发
这是 RAG 系统的核心模块，也是打通整条链路的关键第一步。具体分为：

1. **建立分层骨架**：`controller → service → mapper → model/entity/dto`
2. **先做知识库 CRUD**：最基础但必不可少，验证全链路通畅
3. **再做文档上传**：集成 MinIO，实现文件持久化存储
4. **然后做文档解析**：异步任务，引入 Apache Tika

### ② 同步完善 `rag-common` 的通用能力
- 分页查询的统一 DTO（`PageReqDTO` / `PageResDTO`）
- MyBatis-Plus 雪花 ID 配置
- 切面层的操作日志审计（可选）

### ③ Docker Compose 本地开发环境
确保 PostgreSQL + Redis + MinIO 可一键拉起，降低开发联调门槛。

---

## 💡 建议下一步的具体行动

> [!TIP]
> 建议从 **知识库 CRUD** 开始具体编码，这是投入产出比最高的切入点——代码量适中、架构模式可复用、能快速验证微服务全链路（网关鉴权 → 业务服务 → 数据库）。

**是否需要我立即开始制定 `rag-knowledge` 模块的详细实现计划（Implementation Plan）并开始编码？**
