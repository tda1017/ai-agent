# 登录注册功能测试指南

## 已完成的接入工作

### 1. 后端接口 (已实现)
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册
- `GET /api/auth/getUserInfo` - 获取用户信息

### 2. 前端接入 (已完成)
- ✅ `authService.js` - 已对接真实后端 API
- ✅ `httpClient.js` - 已配置 `/api` 代理
- ✅ `vite.config.js` - 已配置代理到 `http://localhost:8080`
- ✅ 路由守卫 - 未登录自动跳转到登录页

## 测试步骤

### 1. 启动后端服务
```bash
# 在项目根目录
./mvnw spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 2. 启动前端服务
```bash
# 进入前端目录
cd frontend

# 安装依赖（如果还没装）
npm install

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:5173` 启动

### 3. 测试注册功能
1. 访问 `http://localhost:5173/auth/register`
2. 填写注册信息：
   - 用户名: test (3-20字符)
   - 邮箱: test@example.com
   - 密码: test1234 (至少8字符)
   - 确认密码: test1234
3. 勾选同意条款
4. 点击「注册」
5. 注册成功后会自动登录并跳转到首页

### 4. 测试登录功能
1. 访问 `http://localhost:5173/auth/login`
2. 输入注册的用户名和密码
3. 可选：勾选「记住我」
4. 点击「登录」
5. 登录成功后跳转到首页

### 5. 验证认证状态
- 登录后访问 `http://localhost:5173/` 应该能正常访问
- 退出登录后（清除 localStorage 的 token），访问首页会自动跳转到登录页
- 登录状态下访问 `/auth/login` 或 `/auth/register` 会自动跳转到首页

## 数据库要求

确保已执行初始化脚本：
```bash
mysql -u your_username -p your_database < sql/001_init_users.sql
```

## 接口响应格式

### 登录响应
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "test"
    }
  }
}
```

### 注册响应
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "test"
  }
}
```

## 常见问题

### 1. 前端无法连接后端
- 确认后端已启动在 8080 端口
- 检查浏览器控制台的网络请求
- 确认 vite 代理配置正确

### 2. 登录失败
- 确认数据库连接正常
- 确认用户表已创建
- 检查用户名和密码是否正确

### 3. CORS 错误
- 后端已配置 CORS，允许所有来源
- 确认 `WebCorsConfig.java` 配置存在

### 4. Token 无效
- Token 存储在 localStorage 中
- 可以通过浏览器开发工具查看 Application -> Local Storage
- JWT token 格式: `Bearer <token>`
