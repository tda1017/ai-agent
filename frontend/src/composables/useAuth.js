import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'

const user = ref(null)
const token = ref(null)
const loading = ref(false)
const error = ref(null)

export function useAuth() {
  const router = useRouter()
  
  const isAuthenticated = computed(() => !!token.value)
  
  const initAuth = () => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken && storedUser) {
      token.value = storedToken
      try {
        user.value = JSON.parse(storedUser)
      } catch (e) {
        console.error('Failed to parse user data:', e)
        logout()
      }
    }
  }
  
  const login = async (credentials) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await authService.login(credentials)
      token.value = response.token
      user.value = response.user
      
      localStorage.setItem('token', response.token)
      if (response.refreshToken) {
        localStorage.setItem('refreshToken', response.refreshToken)
      }
      localStorage.setItem('user', JSON.stringify(response.user))
      
      const redirect = router.currentRoute.value.query.redirect || '/'
      router.push(redirect)
      
      return response
    } catch (e) {
      error.value = e.response?.data?.message || '登录失败'
      throw e
    } finally {
      loading.value = false
    }
  }
  
  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    
    if (router.currentRoute.value.path !== '/auth/login') {
      router.push('/auth/login')
    }
  }
  
  const register = async (userData) => {
    loading.value = true
    error.value = null
    
    try {
      await authService.register(userData)
      await login({ 
        username: userData.username, 
        password: userData.password 
      })
    } catch (e) {
      error.value = e.response?.data?.message || '注册失败'
      throw e
    } finally {
      loading.value = false
    }
  }
  
  const updateUser = (updates) => {
    if (user.value) {
      user.value = { ...user.value, ...updates }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }
  
  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    initAuth,
    login,
    logout,
    register,
    updateUser
  }
}
