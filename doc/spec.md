# 邮件验证码服务规格说明

## 背景
- 移动端应用（Flutter/JS/Swift）需要支持邮箱验证码用于重置密码、修改敏感信息、登录二次验证等操作。
- 当前尚无统一的验证码服务与邮件通道，需要一个可独立部署、易于扩展的后端组件。

## 目标
- 提供一套后端 API，可以生成、发送、验证邮箱验证码，并与现有用户体系集成。
- 支持后续扩展到短信或其他渠道，核心逻辑保持复用。
- 提供速率限制、过期控制、重试限制等安全机制，降低滥用风险。

## 范围
- 涵盖服务端验证码模块、邮件发送流程、客户端调用契约。
- 不包含用户注册登录基础模块的细节实现。

## 角色与服务
- **Client App**：Flutter App、Web 前端（JS）与 iOS（Swift），负责发起验证码请求与提交验证码。
- **Auth API**：现有用户认证服务，负责校验用户身份、执行密码修改等业务。
- **Verification Service**：新建独立后端（可使用 Java/Spring Boot），提供验证码生成、存储、验证逻辑。
- **Email Provider**：阿里云邮件推送（推荐使用 API/SDK）。
- **数据库/缓存**：存储验证码记录、重试次数；Redis 用于短期缓存与限流，关系型数据库用于审计持久化。

## 用户流程（以重置密码为例）
1. 用户在 App 输入邮箱并点击“发送验证码”。
2. Client 调用 `POST /api/v1/verification-codes` 请求发送验证码。
3. Verification Service 校验邮箱是否存在、检查限流策略，生成验证码并保存（只存哈希）。
4. 服务调用阿里云邮件 API 发送包含验证码的邮件模板。
5. 用户收到邮件，在 App 输入验证码和新密码。
6. Client 调用 `POST /api/v1/verification-codes/verify` 验证验证码有效性。
7. 验证成功后，Client 持验证令牌调用 `POST /api/v1/auth/reset-password` 完成密码重置。
8. Service 将验证码状态标记为已使用，后续同一验证码无法再次使用。

## 系统流程图（文字版）
- App → Verification Service：请求发送验证码
- Verification Service → Auth API：可选验证用户状态
- Verification Service → Email Provider：发送邮件
- Email Provider → 用户邮箱：递送包含验证码
- 用户 → App：输入验证码
- App → Verification Service：提交验证码校验
- Verification Service → Auth API：授权后执行后续操作

## API 设计
### 1. 发送验证码 `POST /api/v1/verification-codes`
- **Headers**：`Content-Type: application/json`
- **Body**：
  {
    "channel": "email",
    "recipient": "user@example.com",
    "purpose": "RESET_PASSWORD"
  }
- **逻辑**：
  - 校验邮箱合法性；若需绑定账号则校验账号存在。
  - 检查发送频率（同一邮箱、IP、设备）。
  - 生成 6 位随机数字（或可配置长度/字符集）。
  - 存储验证码记录：哈希后的 code、过期时间、目的、上下文。
  - 调用邮件服务发送模板，模板占位符包含验证码和有效期。
- **响应**（成功）：`202 Accepted`，Body：
  {
    "requestId": "UUID",
    "expiresInSeconds": 300
  }
- **错误**：
  - `400`：邮箱非法、目的不支持。
  - `404`：邮箱未绑定账号（可配置）。
  - `429`：触发限流（邮件发送过于频繁）。
  - `500`：邮件服务失败时记录并返回。

### 2. 验证验证码 `POST /api/v1/verification-codes/verify`
- **Body**：
  {
    "recipient": "user@example.com",
    "purpose": "RESET_PASSWORD",
    "code": "123456"
  }
- **逻辑**：
  - 根据 recipient+purpose 查询最新有效记录。
  - 校验是否过期、是否使用、错误次数是否超限。
  - 对比加密哈希（例如使用 `bcrypt` 或 `HMAC-SHA256`）。
  - 校验通过：生成一次性 `verificationToken`（JWT / 随机串），写入记录并返回。
- **响应**：
  {
    "verificationToken": "opaque-token",
    "expiresInSeconds": 600
  }
- **错误**：
  - `400`：验证码无效或格式错误。
  - `404`：没有匹配记录。
  - `410`：验证码已过期或已被使用。
  - `423`：尝试次数耗尽，强制冷却。

### 3. 使用验证码令牌执行敏感操作
- 以重置密码为例：`POST /api/v1/auth/reset-password`
  {
    "verificationToken": "opaque-token",
    "newPassword": "..."
  }
- Auth API 校验 `verificationToken` 是否有效（可调用 Verification Service 校验或由 JWT 自校验），通过后更新密码并通知 Verification Service 标记令牌为已消费。

## 数据结构
- 表 `verification_codes`
  - `id` (UUID)
  - `recipient` (varchar)
  - `purpose` (enum: RESET_PASSWORD, CHANGE_EMAIL, TWO_FACTOR ...)
  - `code_hash` (varchar)
  - `channel` (enum: email)
  - `status` (enum: PENDING, VERIFIED, CONSUMED, EXPIRED, LOCKED)
  - `expires_at` (timestamp)
  - `attempt_count` (int)
  - `max_attempts` (int, 默认 5)
  - `metadata` (jsonb，可记录 IP、设备、requestId)
  - `created_at`, `updated_at`
- Redis Key：`vc:{purpose}:{recipient}` 保存到期时间、剩余尝试次数，便于快速校验和限流。

## 邮件发送策略
- 推荐使用阿里云邮件推送 API/SDK：
  - 模板示例：`{{username}}，您好，验证码为 {{code}}，有效期 {{expireMinutes}} 分钟。`
  - 模板 ID 从控制台获取；部署前写入配置。
  - 使用专用发信地址＋别名，配置 SPF/DKIM，确保可达率。
- 提供抽象接口 `EmailGateway`，便于更换供应商或回退 SMTP。
- 失败重试策略：
  - API 调用失败重试最多 3 次，使用指数退避。
  - 超过重试仍失败：记录告警，返回客户端稍后重试。

## 安全与合规
- 验证码只存哈希；邮件正文包含验证码原文。
- 限制同一邮箱每小时最多发送 N 次；同一 IP 每分钟 M 次。
- 验证失败 N 次后锁定并返回 423，等待冷却时间。
- 所有敏感操作需 HTTPS；接口应校验 CSRF/token（Web）。
- 记录审计日志：谁、何时、什么操作、状态。

## 客户端需求
- 在发送按钮上显示倒计时，冷却时间与后端配置一致。
- 表单校验：邮箱格式、验证码长度、新密码强度。
- 错误提示与后端错误码对应。
- 对于 Flutter/JS/Swift，统一使用 REST API，必要时在共享模块中封装请求逻辑。

## 配置与部署
- Verification Service 可作为独立 Java 应用部署，与主应用通过 RPC/REST 通信。
- 环境变量：
  - `ALIYUN_ACCESS_KEY`, `ALIYUN_SECRET`, `ALIYUN_TEMPLATE_ID`
  - `VERIFICATION_CODE_LENGTH`, `VERIFICATION_CODE_TTL`
  - `RATE_LIMIT_PER_EMAIL`, `RATE_LIMIT_PER_IP`
- 监控指标：发送成功率、平均延迟、失败分布、限流次数。

## 测试要点
- 单元测试：验证码生成、存储、过期、哈希校验、错误次数。
- 集成测试：调用发送→邮件网关 mock→验证流程。
- E2E 测试：客户端模拟请求+重置密码成功、过期、错误次数超限、限流场景。
- 负载测试：验证在高并发下的限流与 Redis 命中率。

## 开放问题
- 是否需要支持多语言模板？
- 是否允许多个验证码同时有效（默认仅保留最新一条）。
- 是否需要向管理员暴露审核/查询界面？
- 是否扩展到短信或语音渠道，抽象接口是否足够？

{
"env": {
"ANTHROPIC_AUTH_TOKEN": "sk-faAdLPyZoHxK8kzVDdXDfhAtLkTY3Xoy",
"ANTHROPIC_BASE_URL": "https://api.packycode.com",
"CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC":1

},
"permissions": {
"allow": [],
"deny": []
},
"apiKeyHelper": "echo 'sk-faAdLPyZoHxK8kzVDdXDfhAtLkTY3Xoy'"
}
