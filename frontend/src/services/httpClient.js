import axios from 'axios'
import authService from './authService'
import { CODE_OK, messageOf } from '../utils/errorCodes'

const httpClient = axios.create({
  baseURL: '/api',
  timeout: 20000
})

httpClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 调试日志：观察请求方法/URL/是否带了授权头
    try {
      const hasAuth = !!config.headers.Authorization
      console.debug('[httpClient][request]', {
        method: config.method?.toUpperCase(),
        baseURL: config.baseURL,
        url: config.url,
        hasAuthHeader: hasAuth
      })
    } catch (_) {}
    return config
  },
  (error) => Promise.reject(error)
)

httpClient.interceptors.response.use(
  (response) => {
    // 放宽业务成功判断：
    // - 如果返回的是数组或无 code 普通对象，则直接放行
    // - 仅当返回对象包含 code 字段时，按约定校验
    const resp = response?.data
    if (Array.isArray(resp)) return response
    if (resp && typeof resp === 'object') {
      if (Object.prototype.hasOwnProperty.call(resp, 'code')) {
        if (resp.code === CODE_OK) return response
        const err = { code: resp.code, message: messageOf(resp.code, resp.message) }
        return Promise.reject(err)
      }
      return response
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config
    
    // 仅在 401 时处理认证过期
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      // Token 过期，清空并重定向到登录页
      localStorage.clear()
      
      // 避免在登录页面无限循环重定向
      if (!window.location.pathname.includes('/auth/login')) {
        window.location.href = '/auth/login'
      }
      
      return Promise.reject({ 
        code: 401, 
        message: '登录已过期，请重新登录' 
      })
    }
    
    // 统一 HTTP 层/网络错误处理
    const status = error?.response?.status
    const code = status ?? error?.code ?? -1
    const message = error?.response?.data?.message || error?.message || '请求失败'
    // 保留响应体用于调试（例如 403/500 时）
    return Promise.reject({ code, message, response: error?.response })
  }
)

export default httpClient