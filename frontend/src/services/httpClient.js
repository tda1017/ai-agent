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
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          const data = await authService.refresh(refreshToken)
          
          localStorage.setItem('token', data.token)
          if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken)
          }
          
          originalRequest.headers.Authorization = `Bearer ${data.token}`
          return httpClient(originalRequest)
        }
      } catch (refreshError) {
        localStorage.clear()
        window.location.href = '/auth/login'
        return Promise.reject(refreshError)
      }
    }
    
    // 统一 HTTP 层/网络错误处理
    const code = error?.code || error?.response?.status || -1
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject({ code, message })
  }
)

export default httpClient