# 前端代码修改执行计划

基于规格文档分析，以下是验证后的实际修改方案，按Linus式优先级排序。

## 【P0 - 立即修复】消除代码重复灾难

### 1. 统一聊天组件架构
**问题**：AppChatView.vue 和 ManusChatView.vue 90%代码重复
**方案**：创建统一ChatInterface组件

```bash
# 创建组件目录
mkdir -p frontend/src/components/chat
mkdir -p frontend/src/composables
```

**修改文件**：
- 新建：`frontend/src/components/chat/ChatInterface.vue` - 统一聊天界面
- 新建：`frontend/src/composables/useChat.js` - 聊天逻辑抽象
- 修改：两个聊天页面改为使用统一组件

### 2. 修复 mockSse.js 编码问题
**问题**：中文注释显示为乱码
**方案**：统一UTF-8编码

## 【P1 - 核心改进】架构优化

### 3. HTTP客户端增强
**当前问题**：
- 硬编码baseURL为'/api'
- 无重试和错误处理机制
- 缺少取消支持

**修改方案**：
```javascript
// frontend/src/services/httpClient.js 增强版
const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000
});

// 添加重试拦截器
// 添加取消支持
// 统一错误处理
```

### 4. 设计令牌系统
**方案**：创建CSS变量系统替代硬编码颜色

```css
/* frontend/src/assets/tokens.css */
:root {
  /* Colors */
  --color-primary-500: #3b82f6;
  --color-gray-50: #f9fafb;

  /* Spacing */
  --space-sm: 8px;
  --space-md: 16px;

  /* Radius */
  --radius-md: 16px;
}
```

## 【P2 - 体验提升】用户体验优化

### 5. 响应式和可访问性
- 移动端适配
- 键盘导航支持
- ARIA标签添加

### 6. SSE连接增强
- 断线重连机制
- 心跳检测
- 更好的错误处理

## 【P3 - 工程化】开发体验

### 7. 代码质量工具
- ESLint + Prettier配置
- 预提交钩子
- 单元测试框架

## 立即开始执行

基于"好品味"原则，先解决最核心的问题：

### 第一步：创建统一聊天组件