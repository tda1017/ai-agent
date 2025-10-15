# Serena 操作记录

记录本仓库重要的工程性变更与实施进度，便于审计与回溯。

## 2025-10-15｜M0 后端认证链路骨架落地

目标：在不破坏现有业务的前提下，完成 JWT 无状态认证的最小闭环（登录→签发→过滤→受保护接口）。

变更摘要：
- 依赖：在 `pom.xml` 新增 Security/JPA/MySQL/JJWT 依赖（`spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`mysql-connector-j`、`io.jsonwebtoken:jjwt-*` 0.12.3）。
- 配置：在 `src/main/resources/application.yml` 增加数据源、JPA 与 `jwt.*` 配置（支持环境变量覆盖）。
- 数据库：新增 `sql/001_init_users.sql`（M0 极简 `users` 表）。
- 实体/仓库：新增 `src/main/java/com/xin/aiagent/entity/User.java`、`repository/UserRepository.java`。
- 安全：新增 `security/JwtTokenProvider.java`（签发/解析）、`security/JwtAuthenticationFilter.java`（鉴权注入）。
- 配置：新增 `config/SecurityConfig.java`（无状态、白名单、`/api/**` 需认证、CORS/CSRF 配置）。
- 接口：新增 `controller/AuthController.java`，提供 `POST /api/auth/login` 与占位 `GET /api/auth/getUserInfo`（原 `/me` 重命名）。

设计取舍：
- M0 仅使用 `users` 单表；`roles/permissions` 先由 JWT 载荷静态注入（`ROLE_USER`、`chat:use`）。
- 统一 JWT 无状态，不引入 Session；暂不实现刷新 Token/黑名单（见 Milestones M2/M3）。
- 保持零破坏：放行 `/api/auth/**` 等白名单，业务控制器无需改动即可受保护。

联调建议：
- 生成演示用户 BCrypt 密码并插入 `users` 表。
- 调用 `POST /api/auth/login` 获取 Token；持 Token 访问 `/api/doChatWithApp`、SSE 路由验证鉴权闭环。

后续项（建议）：
- 丰富 `/api/auth/me` 返回；可选增加 `app.security.enabled` 开关用于灰度。
- M1 引入角色/关联表；M2 开启方法级权限；M3 扩展黑名单/刷新 Token/登录限流等。
