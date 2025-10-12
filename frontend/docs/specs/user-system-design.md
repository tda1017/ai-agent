# AI Agent 用户系统设计规范

> "Bad programmers worry about the code. Good programmers worry about data structures."
> 
> 本文档遵循 Linus Torvalds 的工程哲学：数据结构优先，消除特殊情况，实用主义至上。

## 一、核心判断

### 为什么需要用户系统？
- ✅ **真实问题**：没有用户系统 = 无法持久化数据
- ✅ **用户痛点**：无法保存历史会话，无法跨设备使用
- ✅ **商业需求**：没有用户体系，连付费功能都无法实现
- ❌ **不是**：为了炫技而过度设计

### 设计原则
1. **数据结构第一** - 先设计数据，再考虑功能
2. **消除特殊情况** - 统一处理认证逻辑，避免散落各处
3. **向后兼容** - 新功能不能破坏现有聊天功能
4. **实用主义** - 只做用户真正需要的功能

---

## 二、核心数据结构

### 2.1 用户状态 (UserState)
```javascript
// 单一数据源 - 避免状态分散
const userState = {
  isAuthenticated: false,      // 认证状态
  token: null,                  // JWT token
  refreshToken: null,           // 刷新token（可选）
  user: {
    id: string,
    username: string,
    email: string,
    avatar: string | null,
    createdAt: timestamp
  },
  settings: {
    theme: 'dark' | 'light',
    defaultModel: 'gpt-4' | 'claude',
    temperature: number,        // 0-1
    maxTokens: number
  }
}
```

**设计要点**：
- ❌ 别他妈搞 Vuex/Pinia，这点状态用不着
- ✅ localStorage + composable 就够了
- ✅ token 只在 httpClient 层处理，别到处传

### 2.2 会话结构 (Conversation)
```javascript
// 扁平化设计 - 避免复杂嵌套
const conversation = {
  id: string,                   // UUID
  userId: string,               // 所属用户
  title: string,                // 会话标题（自动生成或用户修改）
  model: string,                // 使用的模型
  messages: [
    {
      id: string,
      role: 'user' | 'assistant' | 'system',
      content: string,
      timestamp: number,
      tokens: number | null     // token消耗（后端返回）
    }
  ],
  createdAt: timestamp,
  updatedAt: timestamp,
  isDeleted: boolean            // 软删除
}
```

**设计要点**：
- ✅ 扁平列表，不搞文件夹嵌套（ChatGPT初期也没有）
- ✅ 软删除机制，避免误操作
- ❌ 不搞什么"收藏"、"标签"这些花里胡哨的东西（先做好基础）

---

## 三、功能模块设计

### Phase 1：认证系统（P0 - 必须有）

#### 3.1 登录/注册
**页面**：
- `/auth/login` - 登录页
- `/auth/register` - 注册页

**功能**：
- 邮箱/用户名 + 密码登录
- 注册（带邮箱验证，后端实现）
- Token 自动刷新机制
- "记住我" 功能（延长 token 有效期）

**技术要点**：
```javascript
// useAuth.js - 统一认证入口
export function useAuth() {
  const login = async (credentials) => {
    const { token, user } = await authService.login(credentials)
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    // 设置 httpClient 默认 header
    httpClient.setAuthToken(token)
  }
  
  const logout = () => {
    localStorage.clear()
    httpClient.clearAuthToken()
    router.push('/auth/login')
  }
  
  const checkAuth = () => {
    const token = localStorage.getItem('token')
    if (!token) return false
    // TODO: 验证 token 有效性
    return true
  }
  
  return { login, logout, checkAuth, ... }
}
```

#### 3.2 路由守卫
```javascript
// router/index.js
router.beforeEach((to, from, next) => {
  const publicPages = ['/auth/login', '/auth/register', '/docs']
  const authRequired = !publicPages.includes(to.path)
  const token = localStorage.getItem('token')
  
  if (authRequired && !token) {
    return next('/auth/login')
  }
  
  next()
})
```

**设计要点**：
- ✅ 统一在路由层处理认证，消除各页面的 if/else
- ✅ 公开页面白名单机制
- ❌ 别搞什么角色权限（RBAC），现在用不到

---

### Phase 2：会话管理（P0 - 必须有）

#### 3.3 历史记录侧边栏
**位置**：在现有聊天页面左侧添加侧边栏

**功能**：
- 显示会话列表（按时间倒序）
- 新建会话
- 删除会话（带确认）
- 重命名会话
- 快速搜索（前端过滤）

**UI 参考**：
```
┌─────────────┬──────────────────────┐
│ + 新建会话   │   AI Agent Chat      │
├─────────────┤                      │
│ [搜索框]     │   消息区域            │
│             │                      │
│ ● 今天      │                      │
│  - 会话1    │                      │
│  - 会话2    │                      │
│             │                      │
│ ● 昨天      │                      │
│  - 会话3    │                      │
│             │                      │
│ ● 本周      │                      │
│  - 会话4    │                      │
└─────────────┴──────────────────────┘
```

**技术要点**：
```javascript
// useConversations.js
export function useConversations() {
  const conversations = ref([])
  const currentConversation = ref(null)
  
  const fetchConversations = async () => {
    conversations.value = await conversationService.getAll()
  }
  
  const createConversation = async () => {
    const newConv = await conversationService.create({
      title: '新会话',
      model: userSettings.defaultModel
    })
    conversations.value.unshift(newConv)
    currentConversation.value = newConv
  }
  
  const deleteConversation = async (id) => {
    await conversationService.delete(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
  }
  
  return { conversations, currentConversation, ... }
}
```

#### 3.4 会话持久化
- 每条消息发送后自动保存到后端
- 页面刷新后恢复当前会话
- 支持跨设备同步

---

### Phase 3：用户功能（P1 - 应该有）

#### 3.5 个人资料页
**路径**：`/user/profile`

**功能**：
- 查看/编辑用户名
- 上传头像
- 修改密码
- 查看账号创建时间
- Token 使用统计（后端提供）

#### 3.6 设置页面
**路径**：`/user/settings`

**功能**：
- 默认模型选择
- Temperature 调节（滑块）
- Max Tokens 设置
- 主题切换（Dark/Light）
- 快捷键设置（可选）

**设计要点**：
- ✅ 设置即时生效，无需"保存"按钮
- ✅ 使用合理的默认值
- ❌ 别搞太多参数，用户不关心

#### 3.7 使用文档页
**路径**：`/docs`

**内容**：
- 快速开始指南
- 功能介绍
- 常见问题（FAQ）
- API 使用说明（如果开放）
- 更新日志

**设计要点**：
- ✅ 使用 Markdown 编写，方便维护
- ✅ 支持搜索和目录导航
- ✅ 无需登录即可访问

---

## 四、后端 API 接口定义

### 4.1 认证接口

#### POST /api/auth/register
注册新用户

**Request**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**Response**:
```json
{
  "success": true,
  "message": "注册成功，请验证邮箱",
  "data": {
    "userId": "string"
  }
}
```

#### POST /api/auth/login
用户登录

**Request**:
```json
{
  "username": "string",  // 支持邮箱或用户名
  "password": "string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "token": "JWT_TOKEN",
    "refreshToken": "REFRESH_TOKEN",
    "user": {
      "id": "string",
      "username": "string",
      "email": "string",
      "avatar": "string | null",
      "createdAt": "timestamp"
    }
  }
}
```

#### POST /api/auth/logout
用户登出（可选，前端可直接清除 token）

**Request**:
```json
{
  "token": "string"
}
```

**Response**:
```json
{
  "success": true,
  "message": "登出成功"
}
```

#### POST /api/auth/refresh
刷新 Token

**Request**:
```json
{
  "refreshToken": "string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "token": "NEW_JWT_TOKEN",
    "refreshToken": "NEW_REFRESH_TOKEN"
  }
}
```

#### GET /api/auth/me
获取当前用户信息（需要认证）

**Headers**:
```
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "username": "string",
    "email": "string",
    "avatar": "string | null",
    "createdAt": "timestamp",
    "settings": {
      "theme": "dark",
      "defaultModel": "gpt-4",
      "temperature": 0.7,
      "maxTokens": 2000
    }
  }
}
```

---

### 4.2 用户接口

#### GET /api/user/profile
获取用户资料（需要认证）

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "username": "string",
    "email": "string",
    "avatar": "string | null",
    "bio": "string | null",
    "createdAt": "timestamp",
    "stats": {
      "totalConversations": 123,
      "totalMessages": 4567,
      "totalTokens": 1234567
    }
  }
}
```

#### PUT /api/user/profile
更新用户资料（需要认证）

**Request**:
```json
{
  "username": "string",
  "bio": "string",
  "avatar": "string (base64 或 URL)"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "username": "string",
    "avatar": "string"
  }
}
```

#### GET /api/user/settings
获取用户设置（需要认证）

**Response**:
```json
{
  "success": true,
  "data": {
    "theme": "dark",
    "defaultModel": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 2000
  }
}
```

#### PUT /api/user/settings
更新用户设置（需要认证）

**Request**:
```json
{
  "theme": "dark | light",
  "defaultModel": "string",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "theme": "dark",
    "defaultModel": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 2000
  }
}
```

---

### 4.3 会话接口

#### GET /api/conversations
获取用户的所有会话（需要认证）

**Query Parameters**:
- `page`: 页码（默认 1）
- `limit`: 每页数量（默认 50）
- `sortBy`: 排序字段（默认 `updatedAt`）
- `order`: 排序方向（`asc` | `desc`，默认 `desc`）

**Response**:
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": "string",
        "userId": "string",
        "title": "string",
        "model": "string",
        "messageCount": 10,
        "createdAt": "timestamp",
        "updatedAt": "timestamp"
      }
    ],
    "pagination": {
      "total": 123,
      "page": 1,
      "limit": 50
    }
  }
}
```

#### POST /api/conversations
创建新会话（需要认证）

**Request**:
```json
{
  "title": "string",
  "model": "string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "userId": "string",
    "title": "string",
    "model": "string",
    "messages": [],
    "createdAt": "timestamp",
    "updatedAt": "timestamp"
  }
}
```

#### GET /api/conversations/:id
获取会话详情（需要认证）

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "userId": "string",
    "title": "string",
    "model": "string",
    "messages": [
      {
        "id": "string",
        "role": "user | assistant | system",
        "content": "string",
        "timestamp": "number",
        "tokens": 123
      }
    ],
    "createdAt": "timestamp",
    "updatedAt": "timestamp"
  }
}
```

#### PUT /api/conversations/:id
更新会话（需要认证）

**Request**:
```json
{
  "title": "string",
  "model": "string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "title": "string",
    "model": "string",
    "updatedAt": "timestamp"
  }
}
```

#### DELETE /api/conversations/:id
删除会话（需要认证）

**Response**:
```json
{
  "success": true,
  "message": "会话已删除"
}
```

#### POST /api/conversations/:id/messages
添加消息到会话（需要认证）

**Request**:
```json
{
  "role": "user | assistant",
  "content": "string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "role": "user",
    "content": "string",
    "timestamp": "number",
    "tokens": 123
  }
}
```

---

### 4.4 聊天接口（已存在，需适配）

#### POST /api/chat/stream
流式聊天（需要认证）

**Request**:
```json
{
  "conversationId": "string",  // 新增：关联会话
  "messages": [
    {
      "role": "user | assistant | system",
      "content": "string"
    }
  ],
  "model": "string",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

**Response**: Server-Sent Events (SSE)
```
data: {"content": "Hello", "done": false}
data: {"content": " world", "done": false}
data: {"content": "!", "done": true, "tokens": 123}
```

---

## 五、前端目录结构

```
frontend/
├── src/
│   ├── views/
│   │   ├── auth/
│   │   │   ├── LoginView.vue           # 登录页
│   │   │   └── RegisterView.vue        # 注册页
│   │   ├── user/
│   │   │   ├── ProfileView.vue         # 个人资料
│   │   │   └── SettingsView.vue        # 设置
│   │   ├── docs/
│   │   │   └── DocsView.vue            # 文档页
│   │   ├── history/
│   │   │   └── HistoryView.vue         # 历史记录页（可选）
│   │   ├── HomeView.vue                # 首页
│   │   ├── AppChatView.vue             # AI应用聊天（已存在）
│   │   └── ManusChatView.vue           # 超级智能体聊天（已存在）
│   │
│   ├── components/
│   │   ├── auth/
│   │   │   ├── LoginForm.vue           # 登录表单组件
│   │   │   └── RegisterForm.vue        # 注册表单组件
│   │   ├── layout/
│   │   │   ├── Sidebar.vue             # 侧边栏（会话列表）
│   │   │   ├── Header.vue              # 顶部导航（已存在，需改造）
│   │   │   └── UserMenu.vue            # 用户菜单
│   │   ├── chat/                       # 已存在的聊天组件
│   │   └── common/
│   │       ├── Button.vue              # 通用按钮
│   │       ├── Input.vue               # 通用输入框
│   │       └── Modal.vue               # 通用弹窗
│   │
│   ├── composables/
│   │   ├── useAuth.js                  # 认证状态管理
│   │   ├── useConversations.js         # 会话管理
│   │   ├── useUser.js                  # 用户信息管理
│   │   └── useChat.js                  # 聊天功能（已存在）
│   │
│   ├── services/
│   │   ├── authService.js              # 认证 API
│   │   ├── userService.js              # 用户 API
│   │   ├── conversationService.js      # 会话 API
│   │   ├── chatService.js              # 聊天 API（已存在）
│   │   └── httpClient.js               # HTTP 客户端（已存在，需改造）
│   │
│   ├── router/
│   │   └── index.js                    # 路由配置（需添加守卫）
│   │
│   ├── utils/
│   │   ├── storage.js                  # localStorage 封装
│   │   ├── validators.js               # 表单验证
│   │   └── mockSse.js                  # SSE mock（已存在）
│   │
│   └── assets/
│       └── main.css                    # 全局样式（已存在）
│
└── docs/
    └── specs/
        └── user-system-design.md       # 本文档
```

---

## 六、实施计划

### Phase 1: 认证基础（2天）
**目标**：用户可以注册、登录、登出

**任务**：
1. ✅ 创建 `useAuth.js` composable
2. ✅ 实现登录/注册页面
3. ✅ 改造 `httpClient.js`，添加 token 拦截器
4. ✅ 添加路由守卫
5. ✅ 改造顶部导航，添加用户菜单
6. ✅ 后端实现认证接口

**验收标准**：
- 用户可以注册并登录
- Token 自动添加到请求头
- 未登录用户访问受保护页面时重定向到登录页
- 页面刷新后保持登录状态

---

### Phase 2: 会话管理（2天）
**目标**：用户可以创建、查看、删除会话

**任务**：
1. ✅ 创建 `useConversations.js` composable
2. ✅ 实现侧边栏组件（会话列表）
3. ✅ 改造聊天页面，集成侧边栏
4. ✅ 实现新建/删除/重命名功能
5. ✅ 聊天消息自动关联会话
6. ✅ 后端实现会话接口

**验收标准**：
- 用户可以创建新会话
- 会话列表按时间排序
- 可以切换不同会话
- 删除会话时有确认提示
- 消息持久化到后端

---

### Phase 3: 用户功能（1天）
**目标**：用户可以管理个人资料和设置

**任务**：
1. ✅ 实现个人资料页面
2. ✅ 实现设置页面
3. ✅ 实现头像上传功能
4. ✅ 设置与聊天功能联动（默认模型、参数等）
5. ✅ 后端实现用户接口

**验收标准**：
- 用户可以修改用户名
- 用户可以上传头像
- 设置立即生效
- 查看 Token 使用统计

---

### Phase 4: 文档和优化（1天）
**目标**：完善文档和用户体验

**任务**：
1. ✅ 编写使用文档
2. ✅ 添加快速搜索功能
3. ✅ 添加加载动画和错误提示
4. ✅ 响应式布局优化
5. ✅ 性能优化（懒加载、缓存等）

**验收标准**：
- 文档清晰易懂
- 搜索功能正常
- 移动端体验良好
- 首屏加载时间 < 2s

---

## 七、技术要点

### 7.1 Token 管理
```javascript
// httpClient.js 改造
import axios from 'axios'

const httpClient = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器：自动添加 token
httpClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：自动刷新 token
httpClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config
    
    // Token 过期，尝试刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        
        localStorage.setItem('token', data.token)
        localStorage.setItem('refreshToken', data.refreshToken)
        
        originalRequest.headers.Authorization = `Bearer ${data.token}`
        return httpClient(originalRequest)
      } catch (refreshError) {
        // 刷新失败，跳转登录页
        localStorage.clear()
        window.location.href = '/auth/login'
        return Promise.reject(refreshError)
      }
    }
    
    return Promise.reject(error)
  }
)

export default httpClient
```

**设计要点**：
- ✅ Token 统一在拦截器处理，别到处传
- ✅ 自动刷新机制，用户无感知
- ✅ 刷新失败后清理状态，跳转登录

---

### 7.2 状态管理
```javascript
// composables/useAuth.js
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import httpClient from '@/services/httpClient'

const user = ref(null)
const token = ref(null)

export function useAuth() {
  const router = useRouter()
  
  const isAuthenticated = computed(() => !!token.value)
  
  const initAuth = () => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)
    }
  }
  
  const login = async (credentials) => {
    const response = await authService.login(credentials)
    token.value = response.data.token
    user.value = response.data.user
    
    localStorage.setItem('token', response.data.token)
    localStorage.setItem('refreshToken', response.data.refreshToken)
    localStorage.setItem('user', JSON.stringify(response.data.user))
    
    router.push('/')
  }
  
  const logout = () => {
    token.value = null
    user.value = null
    localStorage.clear()
    router.push('/auth/login')
  }
  
  const register = async (userData) => {
    await authService.register(userData)
    // 注册后自动登录
    await login({ username: userData.username, password: userData.password })
  }
  
  return {
    user,
    token,
    isAuthenticated,
    initAuth,
    login,
    logout,
    register
  }
}
```

**设计要点**：
- ✅ 模块级别的 ref，所有组件共享同一状态
- ✅ 避免使用 Vuex/Pinia（不必要的复杂度）
- ✅ 初始化时从 localStorage 恢复状态

---

### 7.3 路由配置
```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 公开路由
  {
    path: '/auth/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/auth/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/docs',
    name: 'docs',
    component: () => import('@/views/docs/DocsView.vue'),
    meta: { public: true }
  },
  
  // 受保护路由
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/apps',
    name: 'app-chat',
    component: () => import('@/views/AppChatView.vue')
  },
  {
    path: '/manus',
    name: 'manus-chat',
    component: () => import('@/views/ManusChatView.vue')
  },
  {
    path: '/user/profile',
    name: 'profile',
    component: () => import('@/views/user/ProfileView.vue')
  },
  {
    path: '/user/settings',
    name: 'settings',
    component: () => import('@/views/user/SettingsView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const isPublic = to.meta.public
  const token = localStorage.getItem('token')
  
  if (!isPublic && !token) {
    return next({
      path: '/auth/login',
      query: { redirect: to.fullPath }  // 登录后重定向回原页面
    })
  }
  
  if (isPublic && token && to.path.startsWith('/auth')) {
    return next('/')  // 已登录用户访问登录页，重定向到首页
  }
  
  next()
})

export default router
```

**设计要点**：
- ✅ 用 `meta.public` 标记公开路由，避免硬编码路径列表
- ✅ 登录后重定向到原页面（用户体验）
- ✅ 已登录用户不能访问登录/注册页

---

## 八、安全考虑

### 8.1 前端安全
- ✅ Token 存储在 localStorage（XSS 风险可接受）
- ✅ 不在 URL 中传递 token
- ✅ 敏感操作（删除会话、修改密码）二次确认
- ❌ 不在前端做复杂的权限判断（可被绕过）

### 8.2 后端安全（需要后端实现）
- ✅ 使用 bcrypt 加密密码
- ✅ JWT token 设置合理过期时间（1小时）
- ✅ Refresh token 设置长过期时间（7天）
- ✅ 验证 token 有效性和所属用户
- ✅ API 限流（防止暴力破解）
- ✅ 输入验证和 SQL 注入防护
- ✅ CORS 配置（只允许前端域名）

---

## 九、性能优化

### 9.1 首屏加载
- ✅ 路由懒加载（已使用 `() => import()`）
- ✅ 代码分割（Vite 自动处理）
- ✅ 压缩和缓存静态资源

### 9.2 会话列表
- ✅ 分页加载（后端支持）
- ✅ 虚拟滚动（如果会话数 > 100）
- ✅ 防抖搜索（避免频繁请求）

### 9.3 聊天体验
- ✅ 消息流式返回（已实现）
- ✅ 本地消息立即显示（乐观更新）
- ✅ 后台自动保存（debounce）

---

## 十、测试计划

### 10.1 单元测试（可选）
- 认证逻辑测试
- 会话管理逻辑测试
- 工具函数测试

### 10.2 集成测试（必须）
- 登录/注册流程
- 会话创建/删除流程
- 聊天消息持久化

### 10.3 E2E 测试（可选）
- 完整用户流程
- 跨浏览器兼容性

---

## 十一、后续扩展（Phase 5+）

### 可选功能（根据用户反馈决定是否开发）
- 会话导出（Markdown、JSON）
- 会话分享（生成分享链接）
- 提示词模板库
- 多模态输入（图片、文件）
- 语音输入
- 团队协作功能
- API Key 管理（开放 API 调用）

**原则**：先把基础功能做扎实，再考虑扩展。

---

## 总结

这个设计遵循以下原则：

1. **数据结构优先** - 清晰定义 UserState 和 Conversation 结构
2. **消除特殊情况** - 路由守卫和拦截器统一处理认证
3. **实用主义** - 只做用户真正需要的功能
4. **向后兼容** - 不破坏现有聊天功能

**别一次性搞太复杂，按 Phase 1 → 2 → 3 的顺序来，每个 Phase 完成后验收再进入下一个。**

---

_"Talk is cheap. Show me the code."_
_- Linus Torvalds_
