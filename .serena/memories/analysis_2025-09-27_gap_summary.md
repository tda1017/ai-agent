时间: 2025-09-27
主题: 前后端缺口梳理
概要:
- 后端: 无任何 Controller 暴露 HTTP/SSE 接口；与前端约定的 /api/doChatWithApp[+Sse]/doChatWithManus 均缺失；无 CORS 配置；无 application.yml，DashScope/MCP 配置暴露明文；Knife4j 未落地；RAG 本地文档目录 resources/document 缺失。
- 前端: ChatInterface/useChat 已落地（去重完成）；httpClient 仍硬编码 baseURL 且无重试/错误映射；缺少 sseClient 封装；设计 Token（tokens.css）未建立；Vite 别名@缺失；i18n/Pinia/工程化工具链（ESLint/Prettier/Vitest/Playwright）缺失；多处中文乱码（需统一 UTF-8）；SSE manus 端点与 POST 端点同名存在语义冲突。
证据:
- 后端目录空: src/main/java/com/xin/aiagent/controller/（空）
- 前端端点声明: frontend/src/services/chatService.js:4,113,121
- App 服务能力: src/main/java/com/xin/aiagent/app/App.java:35,52,70
- httpClient 基本实现: frontend/src/services/httpClient.js:4,8,11
- Vite 代理: frontend/vite.config.js:7,9
建议:
- 新增 Controller: POST /api/doChatWithApp -> RagCloud；POST /api/doChatWithManus -> Tools；GET SSE /api/doChatWithAppSse & /api/doChatWithManus（或调整前端避免同名冲突）。
- 增加全局 CORS；迁移密钥到环境变量；引入 Knife4j Config；完善输入校验与限流。
- 前端完善 httpClient/sseClient、设计系统、工程化、编码统一与无障碍/响应式。