时间：2025-09-29
任务：将阿里 DashScope API 切换为 DeepSeek（OpenAI 兼容）
触发与偏差：
- Serena check_onboarding_performed 调用失败；Serena list_dir 调用失败，按治理规范 3.9 降级到本地 shell/apply_patch 完成检索与编辑，并在此登记留痕。
变更清单：
- pom.xml：移除 com.alibaba.cloud.ai:spring-ai-alibaba-starter，加入 org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M6。
- src/main/resources/application.yml：删除 spring.ai.dashscope 配置；新增 spring.ai.openai 基于 DeepSeek 的配置（api-key 从环境变量 DEEPSEEK_API_KEY 读取，base-url=https://api.deepseek.com，chat.model=deepseek-chat，embedding.model=deepseek-embedding）。
- src/main/java/com/xin/aiagent/app/App.java：构造器参数从 dashscopeChatModel 改为通用 ChatModel，注入 DeepSeek 兼容实现；新增中文说明注释。
- src/main/java/com/xin/aiagent/rag/AppVectorStoreConfig.java：VectorStore 构建改为通用 EmbeddingModel；新增中文说明注释。
- src/main/java/com/xin/aiagent/rag/AppRagCloudAdvisorConfig.java：移除 DashScope 云检索依赖，改为使用本地 VectorStore 的 QuestionAnswerAdvisor 以维持近似功能。
影响范围：
- 删除了对阿里 DashScope RAG 云索引的直接依赖；doChatWithRagCloud 逻辑现在复用本地向量检索能力。
- 运行时需提供 DEEPSEEK_API_KEY 环境变量。
回滚思路：
- pom.xml 恢复 spring-ai-alibaba-starter 依赖，并移除 openai starter。
- application.yml 恢复 spring.ai.dashscope.* 与模型配置。
- AppRagCloudAdvisorConfig.java 恢复 DashScopeDocumentRetriever 实现与相关 import。
验证建议：
- 本地执行 `mvnw -DskipTests package` 验证编译。
- 以 curl 访问 `/api/doChatWithAppSse?sessionId=xxx&prompt=你好` 检查 DeepSeek 回答。
证据：
- 代码检索命中：App.java、AppRagCloudAdvisorConfig.java、AppVectorStoreConfig.java、application.yml 中均使用了 dashscope/qwen 标记并已清理。