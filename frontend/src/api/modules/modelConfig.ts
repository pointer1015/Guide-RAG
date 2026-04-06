import { http, type BackendResponse } from "../client";

/**
 * 模型配置 API
 * 对接后端 /rag/v1/model-config 接口
 */

/** 模型配置请求 */
export interface ModelConfigRequest {
  provider: string;
  apiKey: string;
  baseUrl: string;
  model: string;
  isActive?: number;
}

/** 模型配置响应 */
export interface ModelConfigResponse {
  id: number;
  provider: string;
  apiKey: string; // 脱敏后的
  baseUrl: string;
  model: string;
  isActive: number;
  gmtCreate: string;
  gmtModified: string;
}

export const modelConfigApi = {
  /**
   * 获取当前用户的模型配置
   * GET /rag/v1/model-config
   */
  async getConfig(): Promise<BackendResponse<ModelConfigResponse | null>> {
    return http.get("/model-config");
  },

  /**
   * 保存模型配置
   * POST /rag/v1/model-config
   */
  async saveConfig(
    data: ModelConfigRequest
  ): Promise<BackendResponse<ModelConfigResponse>> {
    return http.post("/model-config", data);
  },

  /**
   * 删除模型配置（回退默认模型）
   * DELETE /rag/v1/model-config
   */
  async deleteConfig(): Promise<BackendResponse<void>> {
    return http.delete("/model-config");
  },
};
