# 工业级多模态 RAG 知识库聚合助手 API 文档

## 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档版本 | `v1.1.0` |
| 最后更新 | `2026-03-09` |
| 维护人 | 开发团队 |
| 开发环境 Base URL | `http://localhost:8080` |
| 测试环境 Base URL | `http://test-api.example.com` |
| 生产环境 Base URL | `https://api.example.com` |

### 变更历史

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.1.0` | 2026-03-09 | 补全错误码表、完善接口约束、新增缺失接口、修正路由规范 |
| `v1.0.0` | 2026-03-05 | 初版发布 |

---

## 1. 文档说明

- 文档版本：`v1`
- API 前缀：`/rag/v1`
- 认证方式：`JWT Bearer Token`
- 数据格式：
`application/json`（默认）、`multipart/form-data`（文件上传）、`text/event-stream`（SSE）
- 字符编码：`UTF-8`

### 1.1 文档衔接与术语约定

为保证与《开发文档》《数据库设计》《前端开发文档》一致，本文采用以下统一约定：

- 会话与消息资源命名：`sessions`、`messages`。
- 文档解析状态：`PENDING`、`PARSING`、`PARSED`、`FAILED`。
- SSE 事件名：`start`、`delta`、`citation`、`done`、`error`、`ping`。
- Embedding 模型示例默认使用 `bge-m3`。

### 1.2 通用请求头

| Header | 必填 | 示例 | 说明 |
| --- | --- | --- | --- |
| `Content-Type` | 按接口要求 | `application/json` | 请求体类型 |
| `Accept` | 否 | `application/json` | 期望响应类型 |
| `Authorization` | 需登录接口必填 | `Bearer <JWT_TOKEN>` | 访问令牌 |
| `X-Request-Id` | 否 | `9f2b2f0c-2b22-4d45-b3de-38df6f7d1368` | 请求追踪 ID |

### 1.3 通用响应结构（推荐）

```json
{
	"code": "00000",
	"message": "OK",
	"data": {},
	"timestamp": "2026-03-05T10:15:30Z",
	"requestId": "9f2b2f0c-2b22-4d45-b3de-38df6f7d1368"
}
```

### 1.4 通用错误码说明

| HTTP 状态码 | 含义 | 典型场景 |
| --- | --- | --- |
| `400` | Bad Request | 参数缺失、参数格式错误、文件类型不支持 |
| `401` | Unauthorized | 未登录、Token 无效或过期 |
| `403` | Forbidden | 无权限访问当前资源 |
| `404` | Not Found | 会话、知识库、文档不存在 |
| `409` | Conflict | 资源状态冲突，如重复注册 |
| `413` | Payload Too Large | 上传文件超出限制 |
| `415` | Unsupported Media Type | 上传媒体类型不支持 |
| `429` | Too Many Requests | 触发限流 |
| `500` | Internal Server Error | 服务内部异常 |

### 1.5 业务错误码对照表

> 遵循《阿里巴巴Java开发手册》规范：错误码为5位字符串。
> `A` 来源于用户端（如参数校验、未登录、无权限）。
> `B` 来源于当前系统（如内部流控、检索超时、初始化失败）。
> `C` 来源于第三方服务（如大模型调用、TTS 服务）。

| 业务错误码 | 所属级别 | 模块 | message 含义 |
| --- | --- | --- | --- |
| `A0001` | 用户端 | 通用 | `USER_CLIENT_ERROR` — 用户端通用错误 |
| `A0400` | 用户端 | 通用 | `VALIDATION_ERROR` — 请求参数校验失败（含会话、分页等） |
| `A0110` | 用户端 | 认证 | `USERNAME_OR_EMAIL_ALREADY_EXISTS` — 账号名或邮箱已存在 |
| `A0200` | 用户端 | 认证 | `UNAUTHORIZED` — 未鉴权或 Token 缺失 |
| `A0210` | 用户端 | 认证 | `INVALID_CREDENTIALS` — 账号或密码错误 |
| `A0211` | 用户端 | 认证 | `REFRESH_TOKEN_EXPIRED_OR_INVALID` — 刷新令牌过期或无效 |
| `A0212` | 用户端 | 认证 | `REFRESH_TOKEN_REVOKED` — 刷新令牌已被吊销 |
| `A0401` | 用户端 | 知识库 | `FILE_REQUIRED` — 文件或音频不能为空 |
| `A0402` | 用户端 | 知识库 | `UNSUPPORTED_FILE_TYPE` — 不支持的文件或音频格式 |
| `A0403` | 用户端 | 知识库 | `FILE_TOO_LARGE` — 文件超出大小限制 |
| `A0410` | 用户端 | 知识库 | `KNOWLEDGE_BASE_NAME_ALREADY_EXISTS` — 知识库名称已存在 |
| `A0411` | 用户端 | 知识库 | `KNOWLEDGE_BASE_NOT_EMPTY` — 知识库非空，不可删除 |
| `A0500` | 用户端 | 通用 | `RESOURCE_NOT_FOUND` — 知识库、文档或会话不存在 |
| `A0600` | 用户端 | 权限 | `FORBIDDEN_MODIFICATION` — 无权删除或操作资源 |
| `B0001` | 系统侧 | 通用 | `SYSTEM_EXECUTION_ERROR` — 系统级别的内部超时或运行时异常 |
| `B0101` | 系统侧 | 问答 | `STREAM_INIT_FAILED` — SSE 流初始化构建失败 |
| `B0200` | 系统侧 | 问答 | `RETRIEVAL_TIMEOUT` — 向量检索链路耗时超时 |
| `B0300` | 系统侧 | 语音 | `TTS_SERVICE_NOT_ENABLED` — TTS 服务未启用或缺失配置 |
| `C0110` | 第三方 | 语音 | `TRANSCRIPTION_ENGINE_ERROR` — Whisper 转写引擎外部异常 |
| `C0120` | 第三方 | 语音 | `TTS_ENGINE_ERROR` — 语音合成引擎外部调用异常 |

---

## 2. 模块1：用户认证

### 2.1 注册

- 接口名称：用户注册
- URL：`POST /rag/v1/auth/register`
- 权限要求：`无需登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Content-Type` | 是 | `application/json` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `username` | string | 是 | `alice` | 用户名，3-32 字符，仅允许字母、数字、下划线 |
| `email` | string | 是 | `alice@example.com` | 邮箱 |
| `password` | string | 是 | `P@ssw0rd123` | 密码，最短 8 字符，需包含大小写字母与数字 |
| `confirmPassword` | string | 是 | `P@ssw0rd123` | 确认密码 |

```json
{
	"username": "alice",
	"email": "alice@example.com",
	"password": "P@ssw0rd123",
	"confirmPassword": "P@ssw0rd123"
}
```

**成功响应示例（201）**

```json
{
	"code": "00000",
	"message": "REGISTER_SUCCESS",
	"data": {
		"userId": "u_8f3c8a5a",
		"username": "alice",
		"email": "alice@example.com",
		"gmtCreate": "2026-03-05T10:20:00Z"
	},
	"timestamp": "2026-03-05T10:20:00Z",
	"requestId": "9f2b2f0c-2b22-4d45-b3de-38df6f7d1368"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "VALIDATION_ERROR",
	"data": {
		"field": "email",
		"reason": "invalid format"
	}
}
```

`409 Conflict`
```json
{
	"code": "A0110",
	"message": "USERNAME_OR_EMAIL_ALREADY_EXISTS"
}
```

`500 Internal Server Error`
```json
{
	"code": "B0001",
	"message": "INTERNAL_SERVER_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/auth/register" \
	-H "Content-Type: application/json" \
	-d '{
		"username": "alice",
		"email": "alice@example.com",
		"password": "P@ssw0rd123",
		"confirmPassword": "P@ssw0rd123"
	}'
```

### 2.2 登录

- 接口名称：用户登录
- URL：`POST /rag/v1/auth/login`
- 权限要求：`无需登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Content-Type` | 是 | `application/json` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `account` | string | 是 | `alice` | 用户名或邮箱 |
| `password` | string | 是 | `P@ssw0rd123` | 密码 |

```json
{
	"account": "alice",
	"password": "P@ssw0rd123"
}
```

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "LOGIN_SUCCESS",
	"data": {
		"accessToken": "<JWT_ACCESS_TOKEN>",
		"refreshToken": "<JWT_REFRESH_TOKEN>",
		"tokenType": "Bearer",
		"expiresIn": 7200,
		"user": {
			"userId": "u_8f3c8a5a",
			"username": "alice",
			"email": "alice@example.com"
		}
	},
	"timestamp": "2026-03-05T10:25:00Z",
	"requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "VALIDATION_ERROR"
}
```

`401 Unauthorized`
```json
{
	"code": "A0210",
	"message": "INVALID_CREDENTIALS"
}
```

`500 Internal Server Error`
```json
{
	"code": "B0001",
	"message": "INTERNAL_SERVER_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/auth/login" \
	-H "Content-Type: application/json" \
	-d '{
		"account": "alice",
		"password": "P@ssw0rd123"
	}'
```

### 2.3 刷新 Token

- 接口名称：刷新访问令牌
- URL：`POST /rag/v1/auth/refresh`
- 权限要求：`无需登录（需有效 refreshToken）`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Content-Type` | 是 | `application/json` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `refreshToken` | string | 是 | `<JWT_REFRESH_TOKEN>` | 登录返回的刷新令牌 |

```json
{
	"refreshToken": "<JWT_REFRESH_TOKEN>"
}
```

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "TOKEN_REFRESHED",
	"data": {
		"accessToken": "<NEW_JWT_ACCESS_TOKEN>",
		"tokenType": "Bearer",
		"expiresIn": 7200
	},
	"timestamp": "2026-03-05T10:30:00Z",
	"requestId": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0211",
	"message": "REFRESH_TOKEN_EXPIRED_OR_INVALID"
}
```

`403 Forbidden`
```json
{
	"code": "A0212",
	"message": "REFRESH_TOKEN_REVOKED"
}
```

`500 Internal Server Error`
```json
{
	"code": "B0001",
	"message": "INTERNAL_SERVER_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/auth/refresh" \
	-H "Content-Type: application/json" \
	-d '{
		"refreshToken": "<JWT_REFRESH_TOKEN>"
	}'
```

### 2.4 退出

- 接口名称：用户退出登录
- URL：`POST /rag/v1/auth/logout`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 否 | `application/json` |

**请求参数（Body，可选）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `refreshToken` | string | 否 | `<JWT_REFRESH_TOKEN>` | 用于同时吊销刷新令牌 |

```json
{
	"refreshToken": "<JWT_REFRESH_TOKEN>"
}
```

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "LOGOUT_SUCCESS",
	"data": null,
	"timestamp": "2026-03-05T10:35:00Z",
	"requestId": "c3d4e5f6-a7b8-9012-cdef-012345678902"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`500 Internal Server Error`
```json
{
	"code": "B0001",
	"message": "INTERNAL_SERVER_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/auth/logout" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"refreshToken": "<JWT_REFRESH_TOKEN>"
	}'
```

---

## 3. 模块2：会话管理

### 3.1 创建会话

- 接口名称：创建对话会话
- URL：`POST /rag/v1/sessions`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 是 | `application/json` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `title` | string | 否 | `供应链异常分析` | 会话标题 |
| `kbId` | string | 否 | `kb_10001` | 默认关联知识库 |
| `metadata` | object | 否 | `{ "source": "web" }` | 业务扩展字段 |

```json
{
	"title": "供应链异常分析",
	"kbId": "kb_10001",
	"metadata": {
		"source": "web"
	}
}
```

**成功响应示例（201）**

```json
{
	"code": "00000",
	"message": "SESSION_CREATED",
	"data": {
		"sessionId": "s_4e3b9d6f",
		"title": "供应链异常分析",
		"kbId": "kb_10001",
		"gmtCreate": "2026-03-05T10:40:00Z"
	},
	"timestamp": "2026-03-05T10:40:00Z",
	"requestId": "d4e5f6a7-b8c9-0123-defa-123456789034"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "INVALID_SESSION_PARAMS"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/sessions" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"title": "供应链异常分析",
		"kbId": "kb_10001"
	}'
```

### 3.2 获取会话详情

- 接口名称：获取会话详情
- URL：`GET /rag/v1/sessions/{sessionId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | string | 是 | `s_4e3b9d6f` | 会话 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"sessionId": "s_4e3b9d6f",
		"title": "供应链异常分析",
		"kbId": "kb_10001",
		"metadata": { "source": "web" },
		"gmtCreate": "2026-03-05T10:40:00Z",
		"lastMessageAt": "2026-03-05T10:50:00Z"
	},
	"timestamp": "2026-03-05T10:51:00Z",
	"requestId": "b1c2d3e4-f5a6-7890-bcde-123456789012"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "SESSION_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/sessions/s_4e3b9d6f" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 3.3 更新会话

- 接口名称：更新会话标题
- URL：`PATCH /rag/v1/sessions/{sessionId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 是 | `application/json` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | string | 是 | `s_4e3b9d6f` | 会话 ID |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `title` | string | 否 | `新标题` | 更新后的会话标题，1-128 字符 |

```json
{
	"title": "新标题"
}
```

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "SESSION_UPDATED",
	"data": {
		"sessionId": "s_4e3b9d6f",
		"title": "新标题",
		"gmtModified": "2026-03-05T11:00:00Z"
	},
	"timestamp": "2026-03-05T11:00:00Z",
	"requestId": "c2d3e4f5-a6b7-8901-cdef-234567890123"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "SESSION_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X PATCH "http://localhost:8080/rag/v1/sessions/s_4e3b9d6f" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{"title": "新标题"}'
```

### 3.4 获取会话列表（分页）

- 接口名称：会话列表查询
- URL：`GET /rag/v1/sessions`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数（Query）**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | `1` | 页码，从 1 开始 |
| `size` | integer | 否 | `20` | 每页条数，建议 <= 100 |
| `keyword` | string | 否 | `供应链` | 标题关键字模糊搜索 |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"page": 1,
		"size": 20,
		"total": 2,
		"totalPages": 1,
		"items": [
			{
				"sessionId": "s_4e3b9d6f",
				"title": "供应链异常分析",
				"lastMessageAt": "2026-03-05T10:50:00Z",
				"gmtCreate": "2026-03-05T10:40:00Z"
			},
			{
				"sessionId": "s_9012ab34",
				"title": "设备运维问答",
				"lastMessageAt": "2026-03-05T09:30:00Z",
				"gmtCreate": "2026-03-05T09:00:00Z"
			}
		]
	},
	"timestamp": "2026-03-05T10:50:00Z",
	"requestId": "e5f6a7b8-c9d0-1234-efab-234567890345"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "INVALID_PAGINATION_PARAMS"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/sessions?page=1&size=20&keyword=%E4%BE%9B%E5%BA%94%E9%93%BE" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 3.5 删除会话

- 接口名称：删除会话
- URL：`DELETE /rag/v1/sessions/{sessionId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | string | 是 | `s_4e3b9d6f` | 会话 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "SESSION_DELETED",
	"data": {
		"sessionId": "s_4e3b9d6f"
	},
	"timestamp": "2026-03-05T10:55:00Z",
	"requestId": "f6a7b8c9-d0e1-2345-fabc-345678901456"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "SESSION_NOT_FOUND"
}
```

`403 Forbidden`
```json
{
	"code": "A0600",
	"message": "NO_PERMISSION_TO_DELETE_SESSION"
}
```

**cURL 示例**

```bash
curl -X DELETE "http://localhost:8080/rag/v1/sessions/s_4e3b9d6f" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 3.6 获取会话历史消息（分页）

- 接口名称：会话消息历史查询
- URL：`GET /rag/v1/sessions/{sessionId}/messages`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数**

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | string | 是 | `s_4e3b9d6f` | 会话 ID |

Query 参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | `1` | 页码 |
| `size` | integer | 否 | `50` | 每页条数 |
| `role` | string | 否 | `assistant` | 过滤角色：`user/assistant/system` |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"page": 1,
		"size": 50,
		"total": 2,
		"items": [
			{
				"messageId": "m_001",
				"role": "user",
				"content": "近三个月库存周转异常原因是什么？",
				"gmtCreate": "2026-03-05T10:41:00Z"
			},
			{
				"messageId": "m_002",
				"role": "assistant",
				"content": "根据知识库文档，主要原因包括...",
				"citations": [
					{
						"docId": "d_1001",
						"chunkId": "c_3321",
						"sourceText": "库存周转率异常主要受供需波动影响...",
						"score": 0.92
					}
				],
				"gmtCreate": "2026-03-05T10:41:03Z"
			}
		]
	},
	"timestamp": "2026-03-05T10:42:00Z",
	"requestId": "a7b8c9d0-e1f2-3456-abcd-456789012567"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "SESSION_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/sessions/s_4e3b9d6f/messages?page=1&size=50" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

---

## 4. 模块3：知识库与文档管理

### 4.1 创建知识库

- 接口名称：创建知识库
- URL：`POST /rag/v1/knowledge-bases`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 是 | `application/json` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | string | 是 | `智能制造知识库` | 知识库名称 |
| `description` | string | 否 | `用于产线运维与供应链问答` | 描述 |
| `embeddingModel` | string | 否 | `bge-m3` | 向量化模型 |
| `retrievalTopK` | integer | 否 | `5` | 召回条数 |

```json
{
	"name": "智能制造知识库",
	"description": "用于产线运维与供应链问答",
	"embeddingModel": "bge-m3",
	"retrievalTopK": 5
}
```

**成功响应示例（201）**

```json
{
	"code": "00000",
	"message": "KNOWLEDGE_BASE_CREATED",
	"data": {
		"kbId": "kb_10001",
		"name": "智能制造知识库",
		"status": "ACTIVE",
		"gmtCreate": "2026-03-05T11:00:00Z"
	},
	"timestamp": "2026-03-05T11:00:00Z",
	"requestId": "b1c2d3e4-f5a6-7890-bcde-123456789034"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "INVALID_KNOWLEDGE_BASE_PARAMS"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`409 Conflict`
```json
{
	"code": "A0410",
	"message": "KNOWLEDGE_BASE_NAME_ALREADY_EXISTS"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/knowledge-bases" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"name": "智能制造知识库",
		"description": "用于产线运维与供应链问答",
		"embeddingModel": "bge-m3",
		"retrievalTopK": 5
	}'
```

### 4.2 获取知识库列表（分页）

- 接口名称：知识库列表查询
- URL：`GET /rag/v1/knowledge-bases`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数（Query）**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | `1` | 页码，从 1 开始 |
| `size` | integer | 否 | `20` | 每页条数 |
| `keyword` | string | 否 | `制造` | 名称关键字模糊搜索 |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"page": 1,
		"size": 20,
		"total": 1,
		"totalPages": 1,
		"items": [
			{
				"kbId": "kb_10001",
				"name": "智能制造知识库",
				"description": "用于产线运维与供应链问答",
				"embeddingModel": "bge-m3",
				"status": "ACTIVE",
				"docCount": 12,
				"gmtCreate": "2026-03-05T11:00:00Z"
			}
		]
	},
	"timestamp": "2026-03-05T11:05:00Z",
	"requestId": "d3e4f5a6-b7c8-9012-defg-345678901234"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/knowledge-bases?page=1&size=20" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 4.3 获取知识库详情

- 接口名称：获取知识库详情
- URL：`GET /rag/v1/knowledge-bases/{kbId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"kbId": "kb_10001",
		"name": "智能制造知识库",
		"description": "用于产线运维与供应链问答",
		"embeddingModel": "bge-m3",
		"retrievalTopK": 5,
		"status": "ACTIVE",
		"docCount": 12,
		"gmtCreate": "2026-03-05T11:00:00Z",
		"gmtModified": "2026-03-05T11:30:00Z"
	},
	"timestamp": "2026-03-05T11:32:00Z",
	"requestId": "e4f5a6b7-c8d9-0123-efgh-456789012345"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/knowledge-bases/kb_10001" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 4.4 更新知识库

- 接口名称：更新知识库配置
- URL：`PATCH /rag/v1/knowledge-bases/{kbId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 是 | `application/json` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | string | 否 | `新名称` | 知识库名称 |
| `description` | string | 否 | `新描述` | 知识库描述 |
| `retrievalTopK` | integer | 否 | `8` | 召回条数，范围 1~20 |

```json
{
	"name": "新名称",
	"description": "新描述",
	"retrievalTopK": 8
}
```

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "KNOWLEDGE_BASE_UPDATED",
	"data": {
		"kbId": "kb_10001",
		"name": "新名称",
		"description": "新描述",
		"retrievalTopK": 8,
		"gmtModified": "2026-03-05T12:00:00Z"
	},
	"timestamp": "2026-03-05T12:00:00Z",
	"requestId": "f5a6b7c8-d9e0-1234-fghi-567890123456"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

`409 Conflict`
```json
{
	"code": "A0410",
	"message": "KNOWLEDGE_BASE_NAME_ALREADY_EXISTS"
}
```

**cURL 示例**

```bash
curl -X PATCH "http://localhost:8080/rag/v1/knowledge-bases/kb_10001" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{"name": "新名称", "retrievalTopK": 8}'
```

### 4.5 删除知识库

- 接口名称：删除知识库
- URL：`DELETE /rag/v1/knowledge-bases/{kbId}`
- 权限要求：`需要登录`
- 说明：知识库非空时拒绝删除，请先清空文档。

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "KNOWLEDGE_BASE_DELETED",
	"data": {
		"kbId": "kb_10001"
	},
	"timestamp": "2026-03-05T12:10:00Z",
	"requestId": "a6b7c8d9-e0f1-2345-ghij-678901234567"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

`409 Conflict`
```json
{
	"code": "A0411",
	"message": "KNOWLEDGE_BASE_NOT_EMPTY"
}
```

**cURL 示例**

```bash
curl -X DELETE "http://localhost:8080/rag/v1/knowledge-bases/kb_10001" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 4.6 上传文档（PDF/图片/音频）

- 接口名称：上传知识库文档
- URL：`POST /rag/v1/knowledge-bases/{kbId}/documents`
- 权限要求：`需要登录`
- Content-Type：`multipart/form-data`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数**

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |

FormData 参数：

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `file` | file | 是 | `manual.pdf` | 支持 `pdf/png/jpg/jpeg/webp/mp3/wav/m4a`，单文件最大 **50 MB** |
| `title` | string | 否 | `设备维护手册` | 文档标题 |
| `tags` | string | 否 | `运维,设备` | 逗号分隔标签 |
| `language` | string | 否 | `zh-CN` | 文档语言 |

**成功响应示例（201）**

```json
{
	"code": "00000",
	"message": "DOCUMENT_UPLOADED",
	"data": {
		"docId": "d_1001",
		"kbId": "kb_10001",
		"fileName": "manual.pdf",
		"mimeType": "application/pdf",
		"size": 1048576,
		"parseStatus": "PENDING",
		"uploadedAt": "2026-03-05T11:10:00Z"
	},
	"timestamp": "2026-03-05T11:10:00Z",
	"requestId": "c1d2e3f4-a5b6-7890-bcde-123456789012"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0401",
	"message": "FILE_REQUIRED"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

`413 Payload Too Large`
```json
{
	"code": "A0403",
	"message": "FILE_TOO_LARGE"
}
```

`415 Unsupported Media Type`
```json
{
	"code": "A0402",
	"message": "UNSUPPORTED_FILE_TYPE"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/knowledge-bases/kb_10001/documents" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-F "file=@/tmp/manual.pdf" \
	-F "title=设备维护手册" \
	-F "tags=运维,设备" \
	-F "language=zh-CN"
```

### 4.7 获取文档列表

- 接口名称：文档列表查询
- URL：`GET /rag/v1/knowledge-bases/{kbId}/documents`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数**

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |

Query 参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | `1` | 页码 |
| `size` | integer | 否 | `20` | 每页数量 |
| `status` | string | 否 | `PARSED` | 解析状态过滤：`PENDING/PARSING/PARSED/FAILED` |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"page": 1,
		"size": 20,
		"total": 1,
		"totalPages": 1,
		"items": [
			{
				"docId": "d_1001",
				"title": "设备维护手册",
				"fileName": "manual.pdf",
				"parseStatus": "PARSED",
				"chunkCount": 128,
				"gmtModified": "2026-03-05T11:12:00Z"
			}
		]
	},
	"timestamp": "2026-03-05T11:12:00Z",
	"requestId": "d1e2f3a4-b5c6-7890-bcde-123456789012"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "KNOWLEDGE_BASE_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/knowledge-bases/kb_10001/documents?page=1&size=20&status=PARSED" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 4.8 删除文档

- 接口名称：删除文档
- URL：`DELETE /rag/v1/knowledge-bases/{kbId}/documents/{docId}`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |
| `docId` | string | 是 | `d_1001` | 文档 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "DOCUMENT_DELETED",
	"data": {
		"docId": "d_1001"
	},
	"timestamp": "2026-03-05T11:20:00Z",
	"requestId": "e1f2a3b4-c5d6-7890-bcde-123456789012"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`403 Forbidden`
```json
{
	"code": "A0600",
	"message": "NO_PERMISSION_TO_DELETE_DOCUMENT"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "DOCUMENT_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X DELETE "http://localhost:8080/rag/v1/knowledge-bases/kb_10001/documents/d_1001" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

### 4.9 文档解析状态查询

- 接口名称：文档解析状态查询
- URL：`GET /rag/v1/knowledge-bases/{kbId}/documents/{docId}/status`
- 权限要求：`需要登录`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**路径参数**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `kbId` | string | 是 | `kb_10001` | 知识库 ID |
| `docId` | string | 是 | `d_1001` | 文档 ID |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"docId": "d_1001",
		"status": "PARSING",
		"progress": 64,
		"currentStage": "EMBEDDING",
		"errorMessage": null,
		"gmtModified": "2026-03-05T11:15:00Z"
	},
	"timestamp": "2026-03-05T11:15:30Z",
	"requestId": "f2a3b4c5-d6e7-8901-abcd-ef1234567890"
}
```

**错误响应示例**

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "DOCUMENT_NOT_FOUND"
}
```

**cURL 示例**

```bash
curl -X GET "http://localhost:8080/rag/v1/knowledge-bases/kb_10001/documents/d_1001/status" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>"
```

---

## 5. 模块4：核心问答接口（SSE 流式）

### 5.1 发送问题并获取流式回答

- 接口名称：SSE 流式问答
- URL：`POST /rag/v1/chat/stream`
- 权限要求：`需要登录`
- 响应类型：`text/event-stream`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Accept` | 建议 | `text/event-stream` |
| `Cache-Control` | 建议 | `no-cache` |
| `Connection` | 建议 | `keep-alive` |

**请求参数（Body）**

| 参数 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | string | 是 | `s_4e3b9d6f` | 会话 ID |
| `query` | string | 是 | `请总结库存异常的三大原因` | 用户问题 |
| `kbId` | string | 否 | `kb_10001` | 指定检索知识库 |
| `temperature` | number | 否 | `0.2` | 生成温度，范围 `0.0 ~ 1.0`，默认 `0.7` |

```json
{
	"sessionId": "s_4e3b9d6f",
	"query": "请总结库存异常的三大原因",
	"kbId": "kb_10001",
	"temperature": 0.2
}
```

**成功响应示例（200，SSE）**

```text
event: start
data: {"messageId":"m_2001","sessionId":"s_4e3b9d6f","timestamp":"2026-03-05T11:20:00Z"}

event: delta
data: {"delta":"根据知识库中的库存周转数据，"}

event: delta
data: {"delta":"异常主要来自需求预测偏差、补货周期滞后、以及异常退货。"}

event: citation
data: {"citations":[{"docId":"d_1001","chunkId":"c_3321","score":0.92}]}

event: done
data: {"usage":{"promptTokens":516,"completionTokens":143,"totalTokens":659},"finishReason":"stop"}
```

### 5.2 SSE 事件格式规范

服务端按 `SSE` 协议推送事件，每个事件由若干行组成，以空行分隔。

```text
event: <event-name>
id: <event-id>
retry: <milliseconds>
data: <json-string>

```

推荐事件类型：

| `event` | 说明 | 是否重复出现 |
| --- | --- | --- |
| `start` | 流开始元信息 | 否 |
| `delta` | 文本增量分片 | 是 |
| `citation` | 引用文档片段 | 可选 |
| `ping` | 保活心跳 | 可选 |
| `error` | 流式处理异常信息 | 可选 |
| `done` | 流结束元信息 | 否 |

**各事件 `data` 字段结构说明**

`start` 事件：
```json
{"messageId": "m_2001", "sessionId": "s_4e3b9d6f", "timestamp": "2026-03-05T11:20:00Z"}
```

`delta` 事件：
```json
{"delta": "文本增量了"}
```

`citation` 事件：
```json
{"citations": [{"docId": "d_1001", "chunkId": "c_3321", "score": 0.92, "sourceText": "库存周转率异常..."}]}
```

`ping` 事件：
```json
{"timestamp": "2026-03-05T11:20:30Z"}
```

`error` 事件：
```json
{"code": 1499, "message": "RETRIEVAL_TIMEOUT", "detail": "向量检索超时，请重试"}
```

`done` 事件：
```json
{"usage": {"promptTokens": 516, "completionTokens": 143, "totalTokens": 659}, "finishReason": "stop"}
```

### 5.3 连接中断与重试建议

客户端处理建议：

1. 使用 `EventSource` 或支持 SSE 的 HTTP 客户端。
2. 保存最近一次事件 `id`，断线后通过 `Last-Event-ID` 续传。
3. 若服务端返回 `retry: 3000`，客户端按该间隔重连（毫秒）。
4. 当收到 `event: done` 后主动关闭连接，标记消息完成。
5. 若收到 `event: error`，提示用户并允许重新提问。
6. 对 `401` 状态先刷新 Token，再重建 SSE 连接。

前端示例（浏览器）：

```javascript
import { fetchEventSource } from '@microsoft/fetch-event-source'

const token = '<JWT_ACCESS_TOKEN>'

await fetchEventSource('/rag/v1/chat/stream', {
	method: 'POST',
	headers: {
		'Content-Type': 'application/json',
		Accept: 'text/event-stream',
		Authorization: `Bearer ${token}`,
	},
	body: JSON.stringify({
		sessionId: 's_4e3b9d6f',
		query: '请总结库存异常的三大原因',
	}),
	onmessage(event) {
		if (event.event === 'delta') {
			const payload = JSON.parse(event.data)
			console.log(payload.delta)
		}
		if (event.event === 'done') {
			console.log('stream finished')
		}
	},
})
```

> 注意：原生 `EventSource` 无法直接设置 `Authorization` Header。生产环境通常采用以下方案之一：
> 1) 使用网关将 Token 写入 Cookie（HttpOnly + Secure）；
> 2) 使用支持自定义 Header 的 SSE 客户端库（如 `@microsoft/fetch-event-source`）；
> 3) 通过短期签名参数方式鉴权（注意泄漏风险和有效期控制）。

**错误响应示例（非流式阶段）**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "QUERY_OR_SESSION_INVALID"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "A0500",
	"message": "SESSION_NOT_FOUND"
}
```

`500 Internal Server Error`
```json
{
	"code": "B0101",
	"message": "STREAM_INIT_FAILED"
}
```

**cURL 示例**

```bash
curl -N -X POST "http://localhost:8080/rag/v1/chat/stream" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-H "Accept: text/event-stream" \
	-d '{"sessionId":"s_4e3b9d6f","query":"请总结库存异常的三大原因"}'
```

---

## 6. 模块5：语音交互

### 6.1 语音转文字（Whisper）

- 接口名称：语音转写
- URL：`POST /rag/v1/audio/transcriptions`
- 权限要求：`需要登录`
- Content-Type：`multipart/form-data`

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |

**请求参数（FormData）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `file` | file | 是 | `question.wav` | 音频文件，支持 `wav/mp3/m4a` |
| `language` | string | 否 | `zh` | 语种代码 |
| `prompt` | string | 否 | `工业制造场景术语优先` | 识别提示词 |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "TRANSCRIPTION_SUCCESS",
	"data": {
		"text": "请分析最近两周设备停机率上升的原因",
		"language": "zh",
		"duration": 5.32,
		"segments": [
			{
				"start": 0.0,
				"end": 2.5,
				"text": "请分析最近两周"
			},
			{
				"start": 2.5,
				"end": 5.32,
				"text": "设备停机率上升的原因"
			}
		]
	},
	"timestamp": "2026-03-05T12:00:00Z",
	"requestId": "g1h2i3j4-k5l6-7890-bcde-123456789012"
}
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0401",
	"message": "AUDIO_FILE_REQUIRED"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`415 Unsupported Media Type`
```json
{
	"code": "A0402",
	"message": "UNSUPPORTED_AUDIO_FORMAT"
}
```

`500 Internal Server Error`
```json
{
	"code": "C0110",
	"message": "TRANSCRIPTION_ENGINE_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/audio/transcriptions" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-F "file=@/tmp/question.wav" \
	-F "language=zh" \
	-F "prompt=工业制造场景术语优先"
```

### 6.2 语音合成（可选）

- 接口名称：文本转语音
- URL：`POST /rag/v1/audio/synthesis`
- 权限要求：`需要登录`
- 说明：如项目未启用 TTS，返回 `501 Not Implemented`。

**请求头**

| Header | 必填 | 值 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <JWT_ACCESS_TOKEN>` |
| `Content-Type` | 是 | `application/json` |
| `Accept` | 建议 | `audio/mpeg` |

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `text` | string | 是 | `库存异常主要来自预测偏差` | 待合成文本 |
| `voice` | string | 否 | `female_zh_01` | 音色 |
| `format` | string | 否 | `mp3` | 音频格式 |
| `sampleRate` | integer | 否 | `24000` | 采样率 |

```json
{
	"text": "库存异常主要来自预测偏差",
	"voice": "female_zh_01",
	"format": "mp3",
	"sampleRate": 24000
}
```

**成功响应示例（200）**

返回二进制音频流，响应头示例：

```http
Content-Type: audio/mpeg
Content-Disposition: attachment; filename="speech_20260305.mp3"
```

**错误响应示例**

`400 Bad Request`
```json
{
	"code": "A0400",
	"message": "TEXT_REQUIRED"
}
```

`401 Unauthorized`
```json
{
	"code": "A0200",
	"message": "UNAUTHORIZED"
}
```

`404 Not Found`
```json
{
	"code": "B0300",
	"message": "TTS_SERVICE_NOT_ENABLED"
}
```

`500 Internal Server Error`
```json
{
	"code": "C0120",
	"message": "TTS_ENGINE_ERROR"
}
```

**cURL 示例**

```bash
curl -X POST "http://localhost:8080/rag/v1/audio/synthesis" \
	-H "Authorization: Bearer <JWT_ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-H "Accept: audio/mpeg" \
	-d '{
		"text": "库存异常主要来自预测偏差",
		"voice": "female_zh_01",
		"format": "mp3",
		"sampleRate": 24000
	}' \
	--output speech.mp3
```

---

## 7. 模块6：用户信息管理

### 7.1 获取当前用户信息

- 接口名称：获取个人资料
- URL：`GET /rag/v1/users/me`
- 权限要求：`需要登录`

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "OK",
	"data": {
		"userId": "u_8f3c8a5a",
		"username": "alice",
		"email": "alice@example.com",
		"role": "USER",
		"gmtCreate": "2026-03-05T10:20:00Z"
	},
	"timestamp": "2026-03-05T13:00:00Z",
	"requestId": "h1i2j3k4-l5m6-7890-bcde-123456789012"
}
```

### 7.2 更新个人资料

- 接口名称：更新用户信息
- URL：`PATCH /rag/v1/users/me`
- 权限要求：`需要登录`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `email` | string | 否 | `new_alice@example.com` | 新邮箱 |
| `avatar` | string | 否 | `http://...` | 头像 URL |

**成功响应示例（200）**

```json
{
	"code": "00000",
	"message": "USER_UPDATED",
	"data": {
		"userId": "u_8f3c8a5a",
		"email": "new_alice@example.com"
	},
	"timestamp": "2026-03-05T13:05:00Z",
	"requestId": "i2j3k4l5-m6n7-8901-cdef-234567890123"
}
```

---

## 附录

### 附录 A：集成建议

1. 前端统一封装 Token 续期逻辑：接口 `401` 时自动调用 `/auth/refresh`。
2. 对分页接口统一默认 `page=1,size=20`，并约束 `size` 最大值。
3. 上传与 SSE 接口建议单独设置更长超时时间。
4. 对第三方开放时建议增加：`API Key + JWT` 双层鉴权与速率限制。
5. 生产环境建议输出结构化日志并传递 `X-Request-Id` 便于链路追踪。

### 附录 B：Spring Cloud 微服务实现符合度

当前 API 设计已具备较好的资源化基础（`auth/sessions/knowledge-bases/documents/chat`），适合作为 Spring Cloud 微服务拆分的外部契约。为达到最佳实现，建议补充以下治理约束：

1. 网关统一策略：在 `Spring Cloud Gateway` 层统一鉴权、限流、灰度与 `X-Request-Id` 注入。
2. 服务内聚边界：`auth`、`kb+documents`、`chat-orchestrator`、`ingestion` 分别独立发布，避免接口跨域混杂。
3. 版本治理：保持 `/rag/v1` 兼容，新增字段遵循向后兼容，破坏性变更走 `/rag/v2`。
4. 异步任务标准化：文档解析与向量化通过事件总线触发，API 仅暴露任务状态查询，不暴露内部编排细节。
5. 可观测规范：所有响应和日志透传 `requestId/traceId`，支持跨服务链路追踪。

结论：本 API 文档经过术语统一后，已基本符合 Spring Cloud 微服务架构的接口层最佳实践；仍需在运行时治理（注册发现、熔断重试、服务间鉴权）层面配套落地。
