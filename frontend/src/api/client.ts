import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'

/**
 * 后端统一响应结构
 * code为数字，200表示成功
 */
export interface BackendResponse<T = unknown> {
  code: number | string  // 兼容 200 和 "00000"
  message: string
  data: T
  timestamp?: string
  requestId?: string
}

// 创建 axios 实例
// baseURL 设置为 /api，通过 vite proxy 转发到后端 /rag/v1
export const http: AxiosInstance = axios.create({
  baseURL: '/api/rag/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse<BackendResponse>) => {
    const data = response.data
    // 业务成功：code 为 200（后端使用数字）
    if (data.code === 200 || data.code === '200' || data.code === '00000') {
      return data as any
    }
    // 业务错误处理
    console.error('API Error:', data.code, data.message)
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    // HTTP 错误处理
    if (error.response) {
      const status = error.response.status
      const data = error.response.data as BackendResponse | undefined
      const message = data?.message || '请求失败'
      
      switch (status) {
        case 401:
          // 判断是否是公开接口（登录、注册、验证码等不需要认证的接口）
          const url = error.config?.url || ''
          const isPublicApi = url.includes('/auth/login') || 
                             url.includes('/auth/register') || 
                             url.includes('/auth/captcha')
          
          // 只有非公开接口的401才清除登录状态并跳转
          if (!isPublicApi) {
            const userStore = useUserStore()
            userStore.clearAuth()
            // 只有当前不在登录页时才跳转，避免循环
            if (window.location.pathname !== '/login') {
              window.location.href = '/login'
            }
          }
          break
        case 403:
          console.error('没有权限访问此资源:', message)
          break
        case 404:
          console.error('请求的资源不存在:', message)
          break
        case 409:
          console.error('资源冲突:', message)
          break
        case 413:
          console.error('文件过大:', message)
          break
        case 415:
          console.error('不支持的文件类型:', message)
          break
        case 429:
          console.error('请求过于频繁:', message)
          break
        case 500:
          console.error('服务器内部错误:', message)
          break
        default:
          console.error(`请求失败: ${status}`, message)
      }
      return Promise.reject(new Error(message))
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
      return Promise.reject(new Error('网络错误，请检查网络连接'))
    } else {
      console.error('请求配置错误:', error.message)
      return Promise.reject(error)
    }
  }
)

export default http
