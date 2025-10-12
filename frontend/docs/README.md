# AI Agent 前端开发文档

## Phase 1: 认证系统 - 已完成 ✅

### 已实现功能

#### 1. 认证基础设施
- ✅ **useAuth composable** (`src/composables/useAuth.js`)
  - 统一的认证状态管理
  - 模块级别状态共享（无需 Vuex/Pinia）
  - 自动初始化和持久化

- ✅ **Token 管理** (`src/services/httpClient.js`)
  - 请求拦截器：自动添加 Authorization header
  - 响应拦截器：401 自动刷新 token
  - 刷新失败自动重定向到登录页

- ✅ **路由守卫** (`src/router/index.js`)
  - 统一处理未登录用户访问受保护页面
  - 已登录用户访问登录页自动重定向
  - 支持登录后跳转回原页面（redirect query）

#### 2. 认证页面
- ✅ **登录页面** (`/auth/login`)
  - 用户名/密码登录
  - 记住我功能
  - 表单验证
  - 错误提示
  - 演示账号：demo / demo

- ✅ **注册页面** (`/auth/register`)
  - 用户名、邮箱、密码注册
  - 密码确认
  - 实时表单验证
  - 服务条款同意

#### 3. 通用组件
- ✅ **Button 组件** (`src/components/common/Button.vue`)
  - 多种风格：primary、secondary、outline、ghost、danger
  - 加载状态
  - 禁用状态
  - 全宽模式

- ✅ **Input 组件** (`src/components/common/Input.vue`)
  - Label 和必填标记
  - 错误提示和提示文本
  - 多种输入类型支持
  - 自动完成配置

#### 4. 用户菜单
- ✅ **顶部导航用户菜单** (集成在 `App.vue`)
  - 用户头像（首字母）
  - 下拉菜单
  - 用户信息展示
  - 退出登录功能

### 技术实现亮点

#### Token 自动刷新机制
```javascript
// httpClient 响应拦截器
if (error.response?.status === 401 && !originalRequest._retry) {
  // 自动刷新 token
  const data = await authService.refresh(refreshToken)
  // 重试原请求
  return httpClient(originalRequest)
}
```

#### 路由守卫统一认证
```javascript
router.beforeEach((to, from, next) => {
  const isPublic = to.meta.public
  const token = localStorage.getItem('token')
  
  if (!isPublic && !token) {
    return next({ path: '/auth/login', query: { redirect: to.fullPath } })
  }
  next()
})
```

#### 模块级别状态管理
```javascript
// useAuth.js - 避免复杂的状态管理库
const user = ref(null)  // 模块级别，所有组件共享
const token = ref(null)

export function useAuth() {
  // 返回响应式状态和方法
  return { user, token, login, logout, ... }
}
```

### Mock 数据说明

当前所有后端接口均使用 Mock 实现，方便前端开发：

**登录 Mock** (`authService.js`)
- 用户名：`demo`
- 密码：`demo`
- 返回模拟的 token 和用户信息

**注册 Mock**
- 接受任何有效输入
- 注册后自动登录

### 启动开发服务器

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`（或显示的端口）

### 使用流程

1. **首次访问**：未登录会自动跳转到 `/auth/login`
2. **登录**：使用 `demo / demo` 登录
3. **跳转**：登录成功后跳转到首页或之前访问的页面
4. **持久化**：刷新页面保持登录状态
5. **退出**：点击右上角用户菜单 → 退出登录

### 目录结构

```
frontend/src/
├── views/
│   └── auth/
│       ├── LoginView.vue         # 登录页
│       └── RegisterView.vue      # 注册页
├── components/
│   └── common/
│       ├── Button.vue            # 通用按钮
│       └── Input.vue             # 通用输入框
├── composables/
│   └── useAuth.js                # 认证状态管理
├── services/
│   ├── authService.js            # 认证 API（Mock）
│   └── httpClient.js             # HTTP 客户端
└── router/
    └── index.js                  # 路由配置 + 守卫
```

---

## 后端接口对接指南

### 需要后端实现的接口

当后端路径确定后，修改 `src/services/authService.js`：

```javascript
// 将 Mock 实现替换为真实 API 调用
async login(credentials) {
  const { data } = await httpClient.post('/your-backend-path/login', credentials)
  return data
}
```

### 预期接口格式

#### POST /login
**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
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
```

详细的 API 规范见：`docs/specs/user-system-design.md`

---

## 下一步：Phase 2 会话管理

计划功能：
- 历史会话列表侧边栏
- 新建/删除/重命名会话
- 会话持久化
- 消息与会话关联

---

## 开发规范

### 样式规范
- 使用 CSS 变量（见 `assets/main.css`）
- 遵循现有深色主题设计
- 响应式设计（移动端优先）

### 代码规范
- 数据结构优先，避免过度设计
- 统一处理，消除特殊情况（如路由守卫、拦截器）
- 保持函数简洁，单一职责
- 避免不必要的第三方库

### 提交规范
- 功能完成后再提交
- 提交信息清晰描述改动

---

## 故障排除

### 登录失败
- 检查是否使用了 `demo / demo`
- 打开浏览器控制台查看错误信息

### 路由跳转异常
- 检查 localStorage 中是否有 token
- 清除 localStorage 后刷新页面

### 样式异常
- 检查 `assets/main.css` 是否正确加载
- 查看浏览器控制台 CSS 错误

---

_"Talk is cheap. Show me the code." - Linus Torvalds_
