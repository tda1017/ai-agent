时间: 2025-09-27
变更: 实现后端接口层（REST/SSE）与全局 CORS，补充参数校验依赖
文件:
- 新增 src/main/java/com/xin/aiagent/controller/ChatController.java
- 新增 src/main/java/com/xin/aiagent/controller/dto/ChatRequest.java
- 新增 src/main/java/com/xin/aiagent/config/WebCorsConfig.java
- 修改 pom.xml（加入 spring-boot-starter-validation）
概要:
- POST /api/doChatWithApp 与 /api/doChatWithManus：受理请求（ack）
- GET /api/doChatWithAppSse：基于 App.doChatWithRagCloud 流式分片输出
- GET /api/doChatWithManus（SSE）：基于 App.doChatWithTools 流式分片输出（与 POST 同路径但不同方法）
- 统一 CORS: /api/** 开放 GET/POST/OPTIONS（开发态）
验证:
- 本地编译执行 mvnw -DskipTests compile 失败，原因：JDK 版本不匹配（当前环境使用的 JDK 8，依赖需 JDK 17+/21）。需配置 JAVA_HOME 指向 JDK 17+/21 后再验证运行。
回滚: 删除上述新增文件，并从 pom.xml 移除 validation 依赖即可。