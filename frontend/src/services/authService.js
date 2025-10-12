import httpClient from './httpClient'

const authService = {
  async login(credentials) {
    // TODO: 后端路径确定后替换
    // const { data } = await httpClient.post('/auth/login', credentials)
    // return data
    
    // Mock implementation
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (credentials.username === 'demo' && credentials.password === 'demo') {
          resolve({
            token: 'mock-jwt-token-' + Date.now(),
            refreshToken: 'mock-refresh-token-' + Date.now(),
            user: {
              id: '1',
              username: credentials.username,
              email: 'demo@example.com',
              avatar: null,
              createdAt: Date.now()
            }
          })
        } else {
          reject({
            response: {
              data: {
                message: '用户名或密码错误'
              }
            }
          })
        }
      }, 500)
    })
  },
  
  async register(userData) {
    // TODO: 后端路径确定后替换
    // const { data } = await httpClient.post('/auth/register', userData)
    // return data
    
    // Mock implementation
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          message: '注册成功',
          userId: 'mock-user-id-' + Date.now()
        })
      }, 500)
    })
  },
  
  async logout() {
    // TODO: 后端路径确定后替换
    // await httpClient.post('/auth/logout')
    return Promise.resolve()
  },
  
  async refresh(refreshToken) {
    // TODO: 后端路径确定后替换
    // const { data } = await httpClient.post('/auth/refresh', { refreshToken })
    // return data
    
    // Mock implementation
    return Promise.resolve({
      token: 'mock-jwt-token-refreshed-' + Date.now(),
      refreshToken: 'mock-refresh-token-refreshed-' + Date.now()
    })
  },
  
  async getCurrentUser() {
    // TODO: 后端路径确定后替换
    // const { data } = await httpClient.get('/auth/me')
    // return data
    
    // Mock implementation
    const user = localStorage.getItem('user')
    return user ? JSON.parse(user) : null
  }
}

export default authService
