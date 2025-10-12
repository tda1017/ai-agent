import axios from 'axios'
import authService from './authService'

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
  (response) => response,
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
    
    console.error('请求失败', error)
    return Promise.reject(error)
  }
)

export default httpClient