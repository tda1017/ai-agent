# 全栈联调盘点（2025-10-04）

## 结论摘要
- 前后端已具备联调基础：
  - 后端（Spring Boot）已提供 REST + SSE：POST /api/doChatWithApp|Manus；GET /api/doChatWithAppSse、GET /api/doChatWithManus
  - 全局 CORS: WebCorsConfig 允许 /api/** 的 GET/POST/OPTIONS
  - 前端（Vue3 + Vite）已配置代理到 http://localhost:8080，SSE 使用 EventSource 对接上述端点
- 剩余必做项：
  1) 关闭开发态 mock：设置 VITE_USE_MOCK_STREAM=false
  2) 敏感信息迁移：src/main/resources/mcp-servers.json 中的 AMAP_MAPS_API_KEY 迁至环境变量
  3) 统一编码 UTF-8：修复若干中文乱码（ChatInterface.vue、httpClient.js、chatService.js 等）
  4) 生产可配置化：前端 httpClient 与 SSE 基础 URL 支持 VITE_API_BASE_URL；部署时通过反代统一 /api
  5) 后端 DeepSeek 环境变量：DEEPSEEK_API_KEY、DEEPSEEK_BASE_URL、模型名确认

## 证据
- 后端接口：src/main/java/com/xin/aiagent/controller/ChatController.java
- CORS：src/main/java/com/xin/aiagent/config/WebCorsConfig.java
- 前端代理：frontend/vite.config.js；HTTP: frontend/src/services/httpClient.js；SSE: frontend/src/services/chatService.js
- DeepSeek 配置：src/main/resources/application.yml
- 明文密钥：src/main/resources/mcp-servers.json

## 建议验收
- 本地运行：后端 8080、前端 5173，设置 VITE_USE_MOCK_STREAM=false，输入问题可见 SSE 增量输出并最终 done 事件
- 生产运行：反代 /api → 后端，前端构建产物同源部署；或设置 VITE_API_BASE_URL 指向后端域名

## 备注
- 以上内容由 Codex CLI 在本地仓库静态检查产出；未进行外网依赖安装或实际运行验证