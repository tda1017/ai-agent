时间：2025-09-29
问题：启动/调用时抛出 org.springframework.ai.retry.NonTransientAiException: 404 -
根因研判：项目使用 org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M6，目标供应商为 DeepSeek（OpenAI 兼容）。仓库内缺少 application.yml，导致 Chat/Embedding 默认模型仍指向 OpenAI（如 gpt-4o-mini/text-embedding-3-small），DeepSeek 返回 404 model not found。
证据：
- pom.xml 存在 spring-ai-openai-spring-boot-starter:1.0.0-M6。
- 代码 App.java 通过 ChatModel 注入；AppVectorStoreConfig 依赖 EmbeddingModel，均由 starter 自动配置。
- 仓库无 src/main/resources/application.yml（之前计划切换 DeepSeek 的文档存在）。
修复：新增 src/main/resources/application.yml，配置：
- spring.ai.openai.api-key 从 DEEPSEEK_API_KEY/SPRING_AI_OPENAI_API_KEY 读取。
- spring.ai.openai.base-url 默认 https://api.deepseek.com（不带 /v1）。
- spring.ai.openai.chat.options.model=deepseek-chat；embedding.options.model=deepseek-embedding。
- 临时打开 DEBUG 日志便于排障。
影响范围：Chat 与 Embedding 请求全部切换到 DeepSeek 兼容路径 /v1/chat/completions 与 /v1/embeddings。
回滚思路：删除 application.yml 或改回 OpenAI/其他供应商前缀的配置即可。