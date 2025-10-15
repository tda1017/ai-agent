import httpClient from './httpClient'
import { CODE_OK } from '../utils/errorCodes'

const authService = {
  async login(credentials) {
    const { data } = await httpClient.post('/auth/login', credentials)
    if (data.code === CODE_OK && data.data) {
      return {
        token: data.data.token,
        user: {
          id: data.data.user.id,
          username: data.data.user.username,
          email: null,
          avatar: null,
          createdAt: Date.now()
        }
      }
    }
    throw new Error(data.message || '登录失败')
  },
  
  async register(userData) {
    const { data } = await httpClient.post('/auth/register', userData)
    if (data.code === CODE_OK && data.data) {
      return {
        success: true,
        message: data.message || '注册成功',
        userId: data.data.id
      }
    }
    throw new Error(data.message || '注册失败')
  },
  
  async logout() {
    return Promise.resolve()
  },
  
  async refresh(refreshToken) {
    return Promise.resolve({
      token: 'refreshed-token-' + Date.now()
    })
  },
  
  async getCurrentUser() {
    const { data } = await httpClient.get('/auth/getUserInfo')
    if (data.code === CODE_OK) {
      const user = localStorage.getItem('user')
      return user ? JSON.parse(user) : null
    }
    return null
  }
}

export default authService
