时间：2025-09-29
任务：新增通过环境变量覆盖 DeepSeek 模型与 base-url 的能力
变更：
- application.yml 改为：
  - base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
  - chat.options.model: ${DEEPSEEK_CHAT_MODEL:deepseek-chat}
  - embedding.options.model: ${DEEPSEEK_EMBED_MODEL:deepseek-embedding}
目的：便于随时切换到最新模型或代理网关，无需改代码。
使用：设置环境变量 DEEPSEEK_CHAT_MODEL / DEEPSEEK_EMBED_MODEL / DEEPSEEK_BASE_URL。
回滚：恢复为固定模型字符串与固定 base-url。