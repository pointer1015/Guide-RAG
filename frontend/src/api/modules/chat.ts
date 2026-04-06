import { http, type BackendResponse } from "../client";

/**
 * 聊天模块 API
 * 对接后端 /rag/v1/sessions/* 和 /rag/v1/chat/* 接口
 * 参考：API文档.md 第3节 - 会话管理，第5节 - 核心问答接口
 */

// ============ 请求类型 ============

/** 创建会话请求 */
export interface CreateSessionRequest {
  title?: string;
  knowledgeBaseId?: string;
  metadata?: Record<string, unknown>;
}

/** 更新会话请求 */
export interface UpdateSessionRequest {
  title?: string;
}

/** 发送消息请求（用于非流式） */
export interface SendMessageRequest {
  sessionId: string;
  query: string;
  kbId?: string;
  temperature?: number;
}

/** 流式聊天请求 */
export interface ChatStreamRequest {
  sessionId: string;
  query: string;
  kbId?: string;
  temperature?: number;
}

// ============ 响应类型 ============

/** 会话数据 - 对应后端 SessionResDTO */
export interface SessionData {
  id: string; // 后端 Long → JSON string（防止 JS 精度丢失）
  title: string;
  knowledgeBaseId?: string | null; // 同上，Long → string
  lastMessageAt?: string;
  gmtCreate: string;
  gmtModified?: string;
}

/** 会话列表分页响应 - 对应 PageHelper PageInfo 结构 */
export interface SessionListData {
  list: SessionData[]; // PageHelper 使用 list 字段
  total: number;
  pageNum: number; // PageHelper 使用 pageNum
  pageSize: number; // PageHelper 使用 pageSize
  pages: number; // 总页数
}

/** 消息引用数据 */
export interface CitationData {
  docId: string;
  chunkId: string;
  sourceText?: string;
  score?: number;
}

/** 消息数据 */
export interface MessageData {
  messageId: string;
  role: "user" | "assistant" | "system";
  content: string;
  citations?: CitationData[];
  gmtCreate: string;
}

/** 消息列表分页响应 - 对应 PageHelper PageInfo 结构 */
export interface MessageListData {
  list: MessageData[]; // PageHelper 使用 list 字段，非 items
  total: number;
  pageNum: number; // PageHelper 使用 pageNum，非 page
  pageSize: number; // PageHelper 使用 pageSize，非 size
  pages: number; // 总页数
}

/** 会话列表查询参数 */
export interface GetSessionsParams {
  page?: number;
  size?: number;
  keyword?: string;
}

/** 消息列表查询参数 */
export interface GetMessagesParams {
  page?: number;
  size?: number;
  role?: "user" | "assistant" | "system";
}

export const chatApi = {
  /**
   * 获取会话列表（分页）
   * GET /rag/v1/sessions
   */
  async getSessions(
    params?: GetSessionsParams,
  ): Promise<BackendResponse<SessionListData>> {
    return http.get("/sessions", { params });
  },

  /**
   * 获取会话详情
   * GET /rag/v1/sessions/{sessionId}
   */
  async getSession(sessionId: string): Promise<BackendResponse<SessionData>> {
    return http.get(`/sessions/${sessionId}`);
  },

  /**
   * 创建新会话
   * POST /rag/v1/sessions
   */
  async createSession(
    data: CreateSessionRequest,
  ): Promise<BackendResponse<SessionData>> {
    return http.post("/sessions", data);
  },

  /**
   * 更新会话标题
   * PUT /rag/v1/sessions/{sessionId}
   */
  async updateSession(
    sessionId: string,
    data: UpdateSessionRequest,
  ): Promise<BackendResponse<SessionData>> {
    return http.put(`/sessions/${sessionId}`, data);
  },

  /**
   * 删除会话
   * DELETE /rag/v1/sessions/{sessionId}
   */
  async deleteSession(
    sessionId: string,
  ): Promise<BackendResponse<{ sessionId: string }>> {
    return http.delete(`/sessions/${sessionId}`);
  },

  /**
   * 获取会话历史消息（分页）
   * GET /rag/v1/sessions/{sessionId}/messages
   */
  async getMessages(
    sessionId: string,
    params?: GetMessagesParams,
  ): Promise<BackendResponse<MessageListData>> {
    return http.get(`/sessions/${sessionId}/messages`, { params });
  },
};
