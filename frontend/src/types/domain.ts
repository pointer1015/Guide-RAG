/**
 * 业务领域类型定义
 * 前端特有的业务类型，可能与API类型有所不同
 */

// 聊天消息（前端扩展）
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: ChatSource[]
  status?: 'pending' | 'streaming' | 'completed' | 'error'
  createdAt?: string
}

export interface ChatSource {
  title: string
  chunkId: string
  content?: string
  score?: number
}

// 会话列表项
export interface SessionItem {
  id: string
  title: string
  updatedAt: string
  isActive?: boolean
}

// 知识库文档（前端扩展状态）
export interface KbDocument {
  id: string
  name: string
  size: number
  type: string
  status: 'uploading' | 'PENDING' | 'PARSING' | 'PARSED' | 'FAILED'
  progress: number
  errorMessage?: string
  createdAt: string
}

// 知识库详情
export interface KnowledgeBaseDetail {
  id: string
  name: string
  description?: string
  documentCount: number
  documents: KbDocument[]
  createdAt: string
  updatedAt: string
}

// 主题模式
export type ThemeMode = 'light' | 'dark' | 'system'

// 用户设置
export interface UserSettings {
  theme: ThemeMode
  language: 'zh-CN' | 'en-US'
  sendOnEnter: boolean
  showSources: boolean
}

// 导航菜单项
export interface NavMenuItem {
  key: string
  label: string
  icon?: string
  path?: string
  children?: NavMenuItem[]
}

// Toast 通知
export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  description?: string
  duration?: number
}

// 对话框状态
export interface DialogState {
  visible: boolean
  title?: string
  content?: string
  confirmText?: string
  cancelText?: string
  onConfirm?: () => void | Promise<void>
  onCancel?: () => void
}

// 文件类型
export type AcceptedFileType = 'pdf' | 'doc' | 'docx' | 'md' | 'txt'

export const ACCEPTED_FILE_TYPES: Record<AcceptedFileType, string> = {
  pdf: 'application/pdf',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  md: 'text/markdown',
  txt: 'text/plain'
}

export const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
