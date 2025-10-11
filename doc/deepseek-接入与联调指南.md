# DeepSeek 接入与联调指南

日期：2025-09-29  |  适用范围：本仓库（后端 Spring Boot + 前端 Vite）

## 目标与现状
- 已将原阿里 DashScope 集成切换为 DeepSeek（OpenAI 兼容 API）。
- 后端对话与向量检索均已适配，默认使用 DeepSeek 对话模型与向量模型。
- 前端开发环境默认使用「模拟流」；需关闭后才能真实连通后端。

## 一、前置准备
- DeepSeek API Key：从官方控制台获取并妥善保存。
- 端口：后端默认 `8080`，前端开发端口由 Vite 分配（通常 `5173`）。
- Java/Maven：JDK 21，使用项目内置 `mvnw.cmd`。

## 二、后端配置与启动
后端配置位于 `src/main/resources/application.yml`，已支持通过环境变量覆盖。

- 必需环境变量：
  - `DEEPSEEK_API_KEY`：DeepSeek 密钥。
- 可选环境变量：
  - `DEEPSEEK_CHAT_MODEL`：对话模型（默认 `deepseek-chat`）。示例：`deepseek-reasoner`、`deepseek-coder`。
  - `DEEPSEEK_EMBED_MODEL`：向量模型（默认 `deepseek-embedding`）。
  - `DEEPSEEK_BASE_URL`：API 基础地址（默认 `https://api.deepseek.com`）。如走企业代理网关可覆盖。

Windows PowerShell（当前会话有效）：
```
$env:DEEPSEEK_API_KEY='<你的Key>'
$env:DEEPSEEK_CHAT_MODEL='deepseek-reasoner'    # 可选
$env:DEEPSEEK_EMBED_MODEL='deepseek-embedding'  # 可选
$env:DEEPSEEK_BASE_URL='https://api.deepseek.com'  # 可选
```

启动后端（任选一种）：
```
.\mvnw.cmd spring-boot:run

# 或先打包再运行
.\mvnw.cmd -DskipTests package
java -jar target\ai-agent-0.0.1-SNAPSHOT.jar
```

后端关键接口（均在 `ChatController`）：
- `GET /api/doChatWithAppSse`：SSE 流式对话（基于 RAG）。
- `GET /api/doChatWithManus`：SSE 流式对话（工具模式）。
- `POST /api/doChatWithApp`、`POST /api/doChatWithManus`：仅受理请求并回显，不触发大模型调用。

快速自测（SSE 流式）：
```
curl -N "http://localhost:8080/api/doChatWithAppSse?sessionId=demo&prompt=你好，做个自我介绍"
```

## 三、前端配置与启动
前端位于 `frontend/`，Vite 开发服务器已配置反向代理：
- `vite.config.js` 将 `/api` 代理到 `http://localhost:8080`。

开发环境默认启用「模拟流」（方便无后端时联调 UI），需关闭才能调用真实后端：
- 变量来源：`chatService.js` 中 `VITE_USE_MOCK_STREAM`。
- 关闭方式：设置 `VITE_USE_MOCK_STREAM=false`。

推荐做法（前端根目录执行）：
```
cd frontend
npm install

# 仅当前启动会话关闭模拟流
$env:VITE_USE_MOCK_STREAM='false'

npm run dev
```

此时访问前端页面，发起对话将通过 SSE 调用后端接口。

生产构建：
```
cd frontend
npm run build
# 产物位于 dist/，部署到任意静态服务器
```
生产部署时需在网关/服务器上将前端站点的 `/api/*` 路由转发到后端 `http://<后端域名或IP>:8080/api/*`。

## 四、联调流程（建议）
1) 设置后端环境变量，启动后端，确认 `GET /api/doChatWithAppSse` 能返回流式内容。
2) 前端 `VITE_USE_MOCK_STREAM=false`，`npm run dev` 启动并访问页面。
3) 在聊天界面输入问题，观察逐字流式输出。
4) 如需切换最新模型，在后端设置 `DEEPSEEK_CHAT_MODEL=<最新模型ID>` 并重启后端。

## 五、常见问题排查
- 401/403：检查 `DEEPSEEK_API_KEY` 是否正确、是否在当前会话有效（重启终端或服务）。
- 404/网络错误：检查 `DEEPSEEK_BASE_URL`、代理与网络连通性。
- 429：速率限制，延时重试或降低并发（建议退避 20s）。
- 前端仍使用模拟流：确认 `VITE_USE_MOCK_STREAM=false` 已在当前启动会话生效。
- CORS 问题：开发环境通过 Vite 代理避免跨域；生产环境需在网关正确转发 `/api/*`。

## 六、接口与事件约定（SSE）
- 服务端事件：
  - `message`：数据载荷包含 `{ type: 'start' | 'delta' | 'error', ... }`。
  - `done`：结束事件。
- 前端处理：
  - 通用 `onmessage` 处理 `message` 事件；
  - 监听 `done` 事件作为流结束标志。

## 七、当前差异说明
- 原“阿里云检索（DashScope 索引）”已替换为「本地向量库检索」以保持 RAG 体验；后续如需云端知识库服务，可另行选型对接。
- `POST /api/doChatWith*` 接口仅受理请求，不触发大模型调用；实际回答通过对应的 `GET /api/*Sse` 获取。

## 八、验收清单
- [ ] 已设置 `DEEPSEEK_API_KEY`，后端启动无报错。
- [ ] `curl` 访问 SSE 接口可获得流式输出。
- [ ] 前端关闭模拟流后能看到真实的逐字回答。
- [ ] 如需“最新模型”，已设置 `DEEPSEEK_CHAT_MODEL` 并验证生效。

## 九、回滚指引（如需恢复阿里）
- pom.xml 恢复 `com.alibaba.cloud.ai:spring-ai-alibaba-starter`，移除 openai starter。
- application.yml 恢复 `spring.ai.dashscope.*` 配置。
- `AppRagCloudAdvisorConfig.java` 恢复 DashScope 云检索实现。

---
维护者备注：如需我协助将对话模型固定为某个最新 ID、或配置生产网关转发规则，请在工单中附上目标模型名与部署环境信息。

