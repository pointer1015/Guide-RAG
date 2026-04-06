/**
 * API 响应类型定义
 * 与后端接口保持一致
 */

// 通用响应结构
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 用户相关
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname?: string
}

export interface LoginResponse {
  token: string
  user: UserProfile
}

export interface UserProfile {
  id: string
  username: string
  email: string
  nickname: string
  avatar?: string
  createdAt: string
}

// 会话相关
export interface Session {
  id: string
  title: string
  knowledgeBaseId?: string
  createdAt: string
  updatedAt: string
}

export interface CreateSessionRequest {
  title?: string
  knowledgeBaseId?: string
}

// 消息相关
export interface Message {
  id: string
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: Citation[]
  createdAt: string
}

export interface Citation {
  title: string
  chunkId: string
  content?: string
  score?: number
}

export interface ChatRequest {
  sessionId: string
  message: string
  knowledgeBaseId?: string
  stream?: boolean
}

// SSE 事件类型
export type SSEEventType = 'start' | 'delta' | 'citation' | 'done' | 'error' | 'ping'

export interface SSEStartEvent {
  messageId: string
}

export interface SSEDeltaEvent {
  delta: string
}

export interface SSECitationEvent {
  title: string
  chunkId: string
  content?: string
}

export interface SSEDoneEvent {
  messageId: string
  totalTokens?: number
}

export interface SSEErrorEvent {
  code: string
  message: string
}

// 知识库相关
export interface KnowledgeBase {
  id: string
  name: string
  description?: string
  documentCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
}

// 文档相关
export type DocumentStatus = 'PENDING' | 'PARSING' | 'PARSED' | 'FAILED'

export interface Document {
  id: string
  knowledgeBaseId: string
  name: string
  size: number
  type: string
  status: DocumentStatus
  progress: number
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface UploadDocumentResponse {
  id: string
  name: string
  status: DocumentStatus
}

// 文件上传进度
export interface UploadProgress {
  fileId: string
  fileName: string
  progress: number
  status: 'uploading' | 'success' | 'error'
  errorMessage?: string
}
