import { http, type BackendResponse } from "../client";

/**
 * 知识库模块 API
 * 对接后端 /rag/v1/knowledge-bases/* 接口
 * 参考：API文档.md 第4节 - 知识库与文档管理
 */

// ============ 请求类型 ============

/** 创建知识库请求 */
export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
  embeddingModel?: string;
  retrievalTopK?: number;
}

/** 更新知识库请求 */
export interface UpdateKnowledgeBaseRequest {
  name?: string;
  description?: string;
  retrievalTopK?: number;
}

// ============ 响应类型 ============

/** 知识库数据 - 对应后端 KnowledgeBaseResDTO */
export interface KnowledgeBaseData {
  id: string; // 后端 Long → JSON string（防止 JS 精度丢失）
  name: string;
  description?: string;
  createdBy?: string; // 同上，Long → string
  gmtCreate: string;
  gmtModified?: string;
  // 以下字段后端暂未返回，保留供兼容
  docCount?: number;
  status?: string;
}

/** 知识库列表分页响应 - 对应 PageHelper PageInfo 结构 */
export interface KnowledgeBaseListData {
  list: KnowledgeBaseData[]; // PageHelper 使用 list 字段
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/** 文档数据 */
export interface DocumentData {
  id: string; // 后端为 Long id
  kbId?: string;
  title?: string;
  fileName: string;
  mimeType?: string;
  fileSize?: number; // 后端为 fileSize
  parseStatus: "PENDING" | "PARSING" | "PARSED" | "FAILED";
  progress?: number;
  currentStage?: string;
  chunkCount?: number;
  errorMessage?: string | null;
  gmtCreate?: string; // 后端为 gmtCreate
  gmtModified?: string;
}

/** 文档列表分页响应 */
export interface DocumentListData {
  list: DocumentData[]; // PageHelper 使用 list 字段
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/** 文档上传响应 - 后端直接返回文档 ID */
export type UploadDocumentResponseData = string;

/** 文档状态响应 */
export interface DocumentStatusData {
  id: string; // 后端为 Long id
  status: "PENDING" | "PARSING" | "PARSED" | "FAILED";
  progress: number;
  currentStage?: string;
  errorMessage?: string | null;
  gmtModified: string;
}

/** 知识库列表查询参数 */
export interface GetKnowledgeBasesParams {
  page?: number;
  size?: number;
  keyword?: string;
}

/** 文档列表查询参数 */
export interface GetDocumentsParams {
  page?: number;
  size?: number;
  status?: "PENDING" | "PARSING" | "PARSED" | "FAILED";
}

export const knowledgeBaseApi = {
  /**
   * 获取知识库列表（分页）
   * GET /rag/v1/knowledge-bases
   */
  async getKnowledgeBases(
    params?: GetKnowledgeBasesParams,
  ): Promise<BackendResponse<KnowledgeBaseListData>> {
    return http.get("/knowledge-bases", { params });
  },

  /**
   * 获取单个知识库详情
   * GET /rag/v1/knowledge-bases/{kbId}
   */
  async getKnowledgeBase(
    kbId: string,
  ): Promise<BackendResponse<KnowledgeBaseData>> {
    return http.get(`/knowledge-bases/${kbId}`);
  },

  /**
   * 创建知识库
   * POST /rag/v1/knowledge-bases
   */
  async createKnowledgeBase(
    data: CreateKnowledgeBaseRequest,
  ): Promise<BackendResponse<KnowledgeBaseData>> {
    return http.post("/knowledge-bases", data);
  },

  /**
   * 更新知识库
   * PATCH /rag/v1/knowledge-bases/{kbId}
   */
  async updateKnowledgeBase(
    kbId: string,
    data: UpdateKnowledgeBaseRequest,
  ): Promise<BackendResponse<KnowledgeBaseData>> {
    return http.patch(`/knowledge-bases/${kbId}`, data);
  },

  /**
   * 删除知识库
   * DELETE /rag/v1/knowledge-bases/{kbId}
   */
  async deleteKnowledgeBase(
    kbId: string,
  ): Promise<BackendResponse<{ kbId: string }>> {
    return http.delete(`/knowledge-bases/${kbId}`);
  },

  /**
   * 获取知识库文档列表（分页）
   * GET /rag/v1/knowledge-bases/{kbId}/documents
   */
  async getDocuments(
    kbId: string,
    params?: GetDocumentsParams,
  ): Promise<BackendResponse<DocumentListData>> {
    return http.get(`/knowledge-bases/${kbId}/documents`, { params });
  },

  /**
   * 上传文档
   * POST /rag/v1/knowledge-bases/{kbId}/documents
   * Content-Type: multipart/form-data
   */
  async uploadDocument(
    kbId: string,
    file: File,
    options?: {
      title?: string;
      tags?: string;
      language?: string;
      onProgress?: (progress: number) => void;
    },
  ): Promise<BackendResponse<UploadDocumentResponseData>> {
    const formData = new FormData();
    formData.append("file", file);
    if (options?.title) formData.append("title", options.title);
    if (options?.tags) formData.append("tags", options.tags);
    if (options?.language) formData.append("language", options.language);

    return http.post(`/knowledge-bases/${kbId}/documents`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      onUploadProgress: (progressEvent) => {
        if (options?.onProgress && progressEvent.total) {
          const percent = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total,
          );
          options.onProgress(percent);
        }
      },
    });
  },

  /**
   * 获取文档解析状态
   * GET /rag/v1/knowledge-bases/{kbId}/documents/{docId}/status
   */
  async getDocumentStatus(
    kbId: string,
    docId: string,
  ): Promise<BackendResponse<DocumentStatusData>> {
    return http.get(`/knowledge-bases/${kbId}/documents/${docId}/status`);
  },

  /**
   * 删除文档
   * DELETE /rag/v1/knowledge-bases/{kbId}/documents/{docId}
   */
  async deleteDocument(
    kbId: string,
    docId: string,
  ): Promise<BackendResponse<{ docId: string }>> {
    return http.delete(`/knowledge-bases/${kbId}/documents/${docId}`);
  },

  /**
   * 重新处理文档
   * POST /rag/v1/knowledge-bases/{kbId}/documents/{docId}/reprocess
   */
  async reprocessDocument(
    kbId: string,
    docId: string,
  ): Promise<BackendResponse<{ docId: string; status: string }>> {
    return http.post(`/knowledge-bases/${kbId}/documents/${docId}/reprocess`);
  },
};
