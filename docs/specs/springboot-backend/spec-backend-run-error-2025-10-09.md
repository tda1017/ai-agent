# 后端启动报错诊断与修复 Spec

日期：2025-10-09 | 状态：通过

## 概述
- 技术栈：Spring Boot 3.4.9、Java 21、Spring AI 1.0.0-M6（OpenAI 兼容，DeepSeek）。
- 目标：清晰描述“后端运行报错”的现象、根因与修复方案，提供验证步骤与回滚思路，支撑可追溯与可复现。

## 现象与复现
- 运行方式（任一）：
  - `mvn -DskipTests spring-boot:run`
  - 或 `mvn -DskipTests clean package` 后运行打包产物
- 观察：在本地 JDK 为 1.8 的环境下，Maven 构建/运行失败；即便升级 JDK 后，若缺失有效 DeepSeek API Key，则在应用启动期加载向量库（执行 Embedding 调用）时会抛错导致启动中断。

## 根因分析
1) JDK 版本不匹配
   - 证据：`pom.xml` → `<java.version>21</java.version>`，Spring Boot 3.4.x 要求 JDK ≥ 17。
   - 环境：`mvn -v` 显示 `Java version: 1.8.0_401`，与项目要求不符，编译与运行因此失败。

2) 启动期远程嵌入调用导致启动失败（凭据/网络问题）
   - 配置：`src/main/resources/application.yml` 使用 DeepSeek（OpenAI 兼容）。`api-key` 取自环境变量 `DEEPSEEK_API_KEY` 或 `SPRING_AI_OPENAI_API_KEY`。仓库中的默认值为占位 Key（无效）。
   - 代码：`AppVectorStoreConfig.appVectorStore(EmbeddingModel)` 在 Bean 初始化阶段调用 `simpleVectorStore.add(documents)`，需要 `EmbeddingModel` 发起远程嵌入请求。一旦 `API Key` 缺失/无效（401/403）或网络受限，即在启动期抛错，应用无法完成启动。

## 修复方案
### A. 环境修复（必须）
- 安装并切换到 JDK 21（或至少 17，推荐与 `pom.xml` 保持一致为 21）。
  - Windows 示例：
    - 设置环境变量：
      - `setx JAVA_HOME "C:\\Program Files\\Java\\jdk-21"`
      - `setx PATH "%JAVA_HOME%\\bin;%PATH%"`
    - 新开终端确认：`java -version`、`mvn -v` 应显示 21。

### B. 凭据与模型配置（必须）
- 以环境变量方式注入（推荐，避免明文）：
  - `setx DEEPSEEK_API_KEY "<你的真实Key>"`
  - 可选：`setx DEEPSEEK_BASE_URL "https://api.deepseek.com"`
- 模型默认：`deepseek-chat`（对话）、`deepseek-embedding`（向量），可按需通过 `DEEPSEEK_CHAT_MODEL`、`DEEPSEEK_EMBED_MODEL` 覆盖。

### C. 稳健性改造（可选，提升开发体验）
- 目标：即使在无网/无 Key 环境也能启动服务并提供非 RAG/非工具的基本接口能力。
- 建议改造：
  - 将向量库文档嵌入改为“惰性初始化”（首次需要检索/嵌入时再执行）。
  - 或在 `appVectorStore` Bean 中对 `simpleVectorStore.add(documents)` 增加 try/catch，失败仅记录告警，不中断启动；并新增配置开关（例如 `app.vector.init-on-startup: true|false`），默认开发环境为 `false`。
- 注意：改造应保持可配置、可回滚，不改变生产默认行为。

## 验证步骤
1) 版本确认：
   - `java -version` 与 `mvn -v` 显示 JDK 21。
2) 启动后端：
   - `mvn -DskipTests spring-boot:run`
3) 日志检查：
   - 无编译/启动期异常；`org.springframework.ai` 无 401/403。
4) 接口冒烟：
   - 非流式：`POST /api/doChatWithApp`、`POST /api/doChatWithManus`
   - SSE：`GET /api/doChatWithAppSse?sessionId=demo&prompt=你好`
5) 网络：若环境需代理，确保能访问 `https://api.deepseek.com/v1/*`。

## 回滚与风险
- JDK 升级可能影响本机其他旧项目：
  - 可采用 Maven Toolchains 配置项目级 JDK（避免全局切换）。
- 若实施“惰性初始化/失败降级”，需：
  - 提供显式开关，默认与现有行为一致；
  - 记录降级日志，便于排查。

## 影响范围
- 构建与启动阶段：JDK 不符直接阻断编译/启动；
- 启动期 Bean 创建：向量库初始化会触发外部调用，对凭据和网络有强依赖。

## 证据清单（文件/类）
- `pom.xml` → `<java.version>21</java.version>`
- `src/main/resources/application.yml` → DeepSeek/OpenAI 兼容配置（API Key、Base URL、模型）
- `src/main/java/com/xin/aiagent/rag/AppVectorStoreConfig.java` → 启动期嵌入调用
- `src/main/java/com/xin/aiagent/rag/AppDocumentLoader.java` → 文档加载实现
- `src/main/java/com/xin/aiagent/app/App.java`、`ChatController.java` → 业务入口

## 验收标准（SLO/SLI）
- 构建：`mvn -DskipTests clean package` 成功（0 报错）。
- 启动：应用成功启动（端口监听）且无 401/403/网络异常导致的启动失败。
- 功能：上述三个接口冒烟通过；SSE 持续推送并正确结束。

## 后续项（可选）
- 文档：在 `README.md` 增补本地运行前置条件（JDK 21、环境变量、代理设置）。
- 开关：引入 `app.vector.init-on-startup` 控制启动期嵌入行为（默认生产开启、开发关闭）。

