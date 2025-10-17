# 配置指南

## 安全提醒

本项目使用环境变量管理敏感配置。**永远不要将真实的 API Key 或密钥提交到 git。**

## 配置方式（推荐：方式二）

### 方式一：环境变量（最安全，但配置稍复杂）

#### 1. 创建本地环境变量文件

复制示例文件：
```bash
cp envrc.example .envrc
```

编辑 `.envrc` 并替换占位符为你的真实凭据：
- `DEEPSEEK_API_KEY`：你的 DeepSeek API 密钥
- `JWT_SECRET`：使用 `openssl rand -base64 32` 生成
- `DB_PASSWORD`：你的 MySQL 数据库密码（如果有）

#### 2. 创建应用配置文件

复制模板配置：
```bash
cp src/main/resources/application.yml.template src/main/resources/application.yml
```

应用程序会自动从环境变量中读取配置。

#### 3. 加载环境变量

如果使用 direnv：
```bash
direnv allow
```

或手动加载：
```bash
source .envrc
```

---

### 方式二：本地配置文件（推荐，简单直接）

**注意**：`application.yml` 和 `application-openai-embed.yml` 已被 `.gitignore` 忽略，可以安全地在其中硬编码密钥。

#### 快速配置步骤

1. **复制模板**：
```bash
cp src/main/resources/application.yml.template src/main/resources/application.yml
```

2. **直接编辑 application.yml**，将环境变量替换为真实值：
```yaml
spring:
  ai:
    openai:
      # 直接写你的 DeepSeek API Key
      api-key: sk-你的真实密钥
      base-url: https://api.deepseek.com
      
jwt:
  # 直接写你的 JWT Secret（见下方生成方法）
  secret: 你生成的256位密钥
```

3. **生成 JWT Secret**：
```bash
openssl rand -base64 32
```

**优点**：配置简单，不需要环境变量  
**前提**：确保 `application.yml` 已被 gitignore（已配置✅）

---

## 配置文件说明

| 文件 | 用途 | Git 跟踪 |
|------|------|----------|
| `application.yml.template` | 配置模板（占位符） | ✅ 是 |
| `application.yml` | 你的本地配置（可硬编码） | ❌ 否（已忽略）|
| `application-openai-embed.yml` | OpenAI 嵌入配置（可硬编码） | ❌ 否（已忽略）|
| `envrc.example` | 环境变量示例 | ✅ 是 |
| `.envrc` | 你的真实环境变量 | ❌ 否（已忽略）|

## 必需的配置项

### DeepSeek API
- `api-key`（必需）：你的 DeepSeek API 密钥
- `base-url`（可选）：默认 https://api.deepseek.com
- `model`（可选）：默认 deepseek-chat

### JWT 认证
- `secret`（必需）：256 位密钥，使用 `openssl rand -base64 32` 生成
- `expiration`（可选）：默认 86400000 毫秒（24 小时）

### 数据库
- `password`（可选）：如果数据库没有密码可留空

## 安全最佳实践

1. **永远不要提交密钥**：所有敏感文件已被 gitignore
2. **立即吊销泄露的密钥**：如果不小心提交了密钥，立即在控制台吊销
3. **使用强密钥**：JWT secret 使用 `openssl rand -base64 32` 生成
4. **不同环境使用不同密钥**：开发/测试/生产环境使用不同的 API 密钥

## 常见问题

**Q: 我能直接在 application.yml 中硬编码密钥吗？**  
A: 可以！`application.yml` 已被 gitignore，不会提交到 git。但请确保：
   - 永远不要手动 `git add -f` 强制添加这个文件
   - 不要在公开场合分享这个文件的内容

**Q: JWT_SECRET 忘记了怎么办？**  
A: 重新生成一个新的即可（`openssl rand -base64 32`），旧的 token 会失效，用户需要重新登录。
