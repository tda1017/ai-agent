时间：2025-09-26
操作：基于 frontend/ 代码级审查，在 UI 规范文档新增“附录：代码级审查与定点整改建议（2025-09-26）”。
证据与覆盖：
- 读取文件：package.json、vite.config.js、index.html、src/main.js、src/assets/main.css、src/App.vue、src/router/index.js、src/services/{httpClient.js, chatService.js}、src/views/{HomeView.vue, AppChatView.vue, ManusChatView.vue}；utils/mockSse.js 空文件或不可读。
- 关键发现：
  1) main.css 第 1 行误写 `::root`（应为 `:root`）。
  2) httpClient 无退避/重试与错误映射；baseURL 未与环境变量打通。
  3) chatService SSE 无心跳/重连/取消；mock 实现缺失。
  4) 视图层重复代码较多，a11y 与虚拟滚动未覆盖；App.vue 与 HomeView.vue 存在硬编码颜色。
  5) 缺少质量工程与测试脚本（ESLint/Prettier/stylelint、Vitest、Playwright、husky）。
- 建议内容：按文件落点输出整改项与验收标准（详见文档）。
降级说明：Markdown 编辑继续采用 apply_patch（Serena 对非符号文件编辑受限）。
影响范围：仅文档；未修改源码。
回滚思路：删除新增附录或回退文件版本。
后续：如需要，可据此生成任务清单（带文件落点与代码片段）。