# 全栈联调缺口与实现规格说明

日期：2025-09-27  | 状态：提议

## 背景
本项目为 Spring Boot 后端 + Vue 3 前端的 AI Agent 应用。当前后端核心对话能力已在服务层实现（本地 RAG / 云端 RAG / 工具调用），前端完成聊天视图整合与 SSE 模拟开发。但尚未打通真实联调链路，存在接口层缺失、网络容错不足、配置与安全未落地等问题。

## 范围
- 覆盖：后端接口（REST/SSE）与 CORS、安全配置、RAG资源；前端网络层与 SSE 封装、工程化与编码统一；联调契约与验收标准。
- 不覆盖：UI 设计细节与组件体系（另见 `frontend-ui-improvement-spec.md`）。

## 当前状态盘点（证据）
- 后端能力存在但未暴露接口
  - `src/main/java/com/xin/aiagent/app/App.java:35,52,70` 已提供 `doChatWithRagLocal`、`doChatWithRagCloud`、`doChatWithTools`
  - `src/main/java/com/xin/aiagent/controller` 为空，无 `@RestController` 与路由
  - 未检出 CORS、SSE 相关实现（未发现 `@CrossOrigin`/`SseEmitter`/`text/event-stream`）
- 前端端点与实现
  - 期望的端点：`frontend/src/services/chatService.js:4,97,106,113,121`（`/api/doChatWithAppSse`、`/api/doChatWithManus`、POST `/doChatWithApp|Manus`）
  - `httpClient` 仍硬编码 `baseURL: '/api'` 且错误处理简单：`frontend/src/services/httpClient.js:4,8,11`
  - SSE 直接使用 `EventSource`，缺少统一封装与重连/心跳：`frontend/src/services/chatService.js`
  - 多处中文显示乱码，需统一编码为 UTF-8：如 `frontend/src/components/chat/ChatInterface.vue`、`frontend/src/services/httpClient.js`、`frontend/src/utils/mockSse.js`
- 配置与安全
  - 未发现 `application.yml`；`src/main/resources/mcp-servers.json` 含明文密钥
  - 本地 RAG 期望加载 `classpath:document/*.md`，当前资源缺失：`src/main/java/com/xin/aiagent/rag/AppVectorStoreConfig.java:18`

## 待实现清单

### 后端（P0 优先）
1) 新增 Controller（REST + SSE）
- POST `/api/doChatWithApp` → 转发 `App.doChatWithRagCloud(message, chatId)`
- POST `/api/doChatWithManus` → 转发 `App.doChatWithTools(message, chatId)`
- GET SSE `/api/doChatWithAppSse?sessionId=...&prompt=...` → `text/event-stream`
- 选项：如需工具模式 SSE，新增 `/api/doChatWithManusSse`

2) CORS 与契约
- 全局 CORS 配置，允许前端来源；统一 JSON 错误响应与 SSE 事件语义
- 接口入参使用 DTO + 校验注解；出参对齐前端消费格式

3) 配置与安全
- 新增 `application.yml`（示例）：从环境变量读取 `spring.ai.dashscope.api-key`
- 将 `mcp-servers.json` 中的密钥迁移至环境变量，文件仅保留占位
- 提供示例 `.env` / 文档说明，不提交真实密钥

4) RAG 本地资料
- 补充 `src/main/resources/document/*.md`（如无资料，提供空目录与占位说明，或临时关闭本地 RAG 加载）

5) 健壮性与限流
- 基于 Spring Validation 的输入校验、异常映射；基础限流（如 IP/会话维度）返回 429
- SSE 心跳与断线关闭处理（server 端）

### 前端（P0 优先）
1) 网络层与容错
- `httpClient`：改用 `VITE_API_BASE_URL`；实现 429 固定退避 20s，5xx/超时退避 2s 最多重试 1 次；统一错误码转译与可读提示
- 新增 `src/services/sseClient.js`：封装 EventSource，支持心跳、超时、自动重连（带固定退避）、取消、事件去抖/背压

2) 接口路径与语义
- 分离 SSE 与 POST 路由，避免 `manus` 同名引发歧义；建议 SSE 路由统一前缀 `/api/sse/*`

3) 编码与工程化
- 统一文件编码 UTF-8 无 BOM，修复现有乱码
- Vite 别名 `@` 与按环境代理；补充 ESLint/Prettier/stylelint/Vitest/Playwright、husky + lint-staged

4) 设计与可用性（与前端 UI 规格衔接）
- 引入 tokens.css（颜色/间距/字体层级）；组件再拆分（MessageList/MessageItem/ChatInput/Toolbar）
- a11y：键盘可达、ARIA 属性、对比度；移动端适配

## API 契约（建议稿）

### 1. 创建对话请求（App）
- 路径：POST `/api/doChatWithApp`
- Body：`{ "sessionId": "string", "prompt": "string" }`
- 成功：`202 Accepted` 或 `200 OK`，Body `{ "accepted": true }`
- 失败：
  - 400 参数错误；429 触发限流；5xx 后端异常

### 2. 创建对话请求（Manus/工具）
- 路径：POST `/api/doChatWithManus`
- 语义同上

### 3. 流式应答（SSE）
- 路径：GET `/api/doChatWithAppSse?sessionId=...&prompt=...`
- Header：`Content-Type: text/event-stream; charset=utf-8`
- 事件语义：
  - `message`：`{ "type": "delta", "content": "..." }`
  - `done`：`{ "type": "done" }`（或发送 data: done 并关闭）
  - `error`：`{ "type": "error", "code": "...", "message": "..." }`

备注：如需工具模式 SSE，新增 `/api/doChatWithManusSse`，语义一致。

## 配置与环境
- 后端环境变量
  - `SPRING_AI_DASHSCOPE_API_KEY`（映射 `spring.ai.dashscope.api-key`）
- 前端环境变量
  - `VITE_API_BASE_URL`（如省略则使用 Vite 代理 `/api`）
  - `VITE_USE_MOCK_STREAM`（开发态可切换 mock）

## 验收标准（SLO/SLI）
- 联调：前端关闭 mock 后，SSE 正常流式输出，消息“开始-增量-结束”顺序稳定
- 容错：429 固定退避 20s，5xx/超时退避 2s 且最多重试 1 次均生效
- 安全：密钥不出现在仓库；本地运行通过环境变量注入；CORS 正常放通指定来源
- RAG：本地文档存在且成功加载，或明确禁用本地 RAG 且日志无报错
- 工程化：lint/format/test 通过；关键路径具备最小单测/E2E

## 实施计划（建议顺序）
1) 后端 Controller + CORS + DTO 校验 + SSE（P0）
2) 前端 httpClient 增强 + sseClient 封装 + 路由对齐（P0）
3) 配置迁移与示例（后端 `application.yml` 样例、前端 `.env.*`）（P0）
4) UTF-8 编码统一与乱码修复（P0）
5) RAG 文档目录与加载验证（P1）
6) 工程化与质量工具链补齐（P2）
7) a11y/响应式与设计 Token 落地（P2）

## 回滚与降级策略
- 后端接口变更仅新增，不破坏现有路由；回滚可停用新 Controller Bean 或通过网关/反代屏蔽新路由
- SSE 不稳定时，前端回退至 `VITE_USE_MOCK_STREAM=true` 模式进行开发
- RAG 加载失败时，禁用本地 RAG（不影响云端 RAG 与工具模式）

## 附录：证据与参考
- 能力实现：`src/main/java/com/xin/aiagent/app/App.java:35,52,70`
- 前端端点与 SSE 使用：`frontend/src/services/chatService.js:4,97,106,113,121`
- httpClient 基础实现：`frontend/src/services/httpClient.js:4,8,11`
- 代理配置：`frontend/vite.config.js:7,9`
- RAG 文档加载入口：`src/main/java/com/xin/aiagent/rag/AppVectorStoreConfig.java:18`
- 明文密钥风险：`src/main/resources/mcp-servers.json:1`

