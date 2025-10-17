# Configuration Guide

## Security Notice

This project uses environment variables for sensitive configuration. **Never commit real API keys or secrets to git.**

## Setup Instructions

### 1. Create Local Environment File

Copy the example environment file:
```bash
cp envrc.example .envrc
```

Edit `.envrc` and replace placeholder values with your real credentials:
- `DEEPSEEK_API_KEY`: Your DeepSeek API key
- `JWT_SECRET`: Generate with `openssl rand -base64 32`
- `DB_PASSWORD`: Your MySQL database password (if any)

### 2. Create Application Configuration

Copy the template configuration:
```bash
cp src/main/resources/application.yml.template src/main/resources/application.yml
```

The application will automatically read environment variables from your shell or `.envrc`.

### 3. Load Environment Variables

If using direnv:
```bash
direnv allow
```

Or manually source the file:
```bash
source .envrc
```

## Configuration Files

| File | Purpose | Git Tracked |
|------|---------|-------------|
| `application.yml.template` | Template with env var placeholders | ✅ Yes |
| `application.yml` | Your local config (auto-created) | ❌ No (gitignored) |
| `envrc.example` | Example environment variables | ✅ Yes |
| `.envrc` | Your real environment variables | ❌ No (gitignored) |

## Required Environment Variables

### DeepSeek API
- `DEEPSEEK_API_KEY` (required)
- `DEEPSEEK_BASE_URL` (optional, default: https://api.deepseek.com)
- `DEEPSEEK_CHAT_MODEL` (optional, default: deepseek-chat)
- `DEEPSEEK_EMBED_MODEL` (optional, default: deepseek-embedding)

### JWT Authentication
- `JWT_SECRET` (required) - Generate with: `openssl rand -base64 32`
- `JWT_EXPIRATION` (optional, default: 86400000ms = 24 hours)

### Database
- `DB_PASSWORD` (optional if no password set)

## Security Best Practices

1. **Never commit secrets**: All sensitive files are gitignored
2. **Rotate leaked keys immediately**: If you accidentally commit a key, rotate it ASAP
3. **Use strong secrets**: Generate JWT secret with `openssl rand -base64 32`
4. **Different keys per environment**: Use different API keys for dev/staging/prod
