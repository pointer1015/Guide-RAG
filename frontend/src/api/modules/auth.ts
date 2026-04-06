import { http, type BackendResponse } from "../client";

/**
 * 认证模块 API
 * 对接后端 /rag/v1/auth/* 接口
 * 参考：API文档.md 第2节 - 用户认证
 */

// ============ 请求类型 ============

/** 登录请求 */
export interface LoginRequest {
  email: string; // 后端需要 email 字段
  password: string;
  captchaCode: string; // 验证码
  captchaUuid: string; // 验证码UUID
}

/** 注册请求 */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  captchaCode: string;
  captchaUuid: string;
}

/** 刷新Token请求 */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/** 退出登录请求 */
export interface LogoutRequest {
  refreshToken?: string;
}

// ============ 响应类型 ============

/** 登录响应数据 - 后端实际只返回token字符串 */
export interface LoginResponseData {
  // 后端实际返回的是字符串token，不是对象
  // 需要前端自己解析JWT获取用户信息
  token?: string;
  // 以下是期望的结构，暂时保留
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user?: {
    userId: string;
    username: string;
    email: string;
  };
}

/** 注册响应数据 - 后端注册成功后直接返回 JWT Token 字符串，免去二次登录 */
// export interface RegisterResponseData { ... }  // 已废弃，后端现直接返回 token

/** 刷新Token响应数据 */
export interface RefreshTokenResponseData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

/** 验证码响应数据 */
export interface CaptchaResponseData {
  uuid: string;
  base64Image: string;
}

export const authApi = {
  /**
   * 获取验证码
   * GET /rag/v1/auth/captcha
   */
  async getCaptcha(): Promise<BackendResponse<CaptchaResponseData>> {
    return http.get("/auth/captcha");
  },

  /**
   * 用户登录
   * POST /rag/v1/auth/login
   */
  async login(data: LoginRequest): Promise<BackendResponse<string>> {
    return http.post("/auth/login", data);
  },

  /**
   * 用户注册
   * POST /rag/v1/auth/register
   * 注册成功后后端直接返回 JWT Token，无需再次调用 login
   */
  async register(data: RegisterRequest): Promise<BackendResponse<string>> {
    return http.post("/auth/register", data);
  },

  /**
   * 刷新访问令牌
   * POST /rag/v1/auth/refresh
   */
  async refreshToken(
    data: RefreshTokenRequest,
  ): Promise<BackendResponse<RefreshTokenResponseData>> {
    return http.post("/auth/refresh", data);
  },

  /**
   * 退出登录
   * POST /rag/v1/auth/logout
   */
  async logout(data?: LogoutRequest): Promise<BackendResponse<null>> {
    return http.post("/auth/logout", data || {});
  },

  /**
   * 获取当前用户信息
   * GET /rag/v1/auth/me
   */
  async getMe(): Promise<BackendResponse<any>> {
    return http.get("/auth/me");
  }
};
