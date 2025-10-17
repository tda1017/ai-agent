<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h1 class="brand-text">AI Agent</h1>
        </div>
        <p class="auth-subtitle">登录您的账号</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <Input
          v-model="form.username"
          label="用户名或邮箱"
          type="text"
          placeholder="请输入用户名或邮箱"
          required
          autocomplete="username"
          :error="errors.username"
        />

        <Input
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          required
          autocomplete="current-password"
          :error="errors.password"
        />

        <div class="form-options">
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.remember" />
            <span>记住我</span>
          </label>
          <a href="#" class="link">忘记密码？</a>
        </div>

        <div v-if="error" class="error-alert">
          {{ error }}
        </div>

        <Button type="submit" :loading="loading" block>
          登录
        </Button>
      </form>

      <div class="auth-footer">
        <span class="footer-text">还没有账号？</span>
        <RouterLink to="/auth/register" class="link">立即注册</RouterLink>
      </div>

      <div class="demo-hint">
        <p>演示账号: demo / demo</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'

const { login, loading, error } = useAuth()

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const errors = reactive({
  username: '',
  password: ''
})

const validateForm = () => {
  let isValid = true
  errors.username = ''
  errors.password = ''

  if (!form.username.trim()) {
    errors.username = '请输入用户名或邮箱'
    isValid = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (form.password.length < 4) {
    errors.password = '密码至少4个字符'
    isValid = false
  }

  return isValid
}

const handleLogin = async () => {
  if (!validateForm()) return

  try {
    await login({
      username: form.username,
      password: form.password
    })
  } catch (e) {
    console.error('Login failed:', e)
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-lg);
  background: var(--color-background-gradient);
}

.auth-card {
  width: 100%;
  max-width: 540px;
  padding: var(--space-2xl);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.auth-header {
  text-align: center;
  margin-bottom: var(--space-2xl);
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-text-primary);
  border-radius: var(--radius-sm);
  padding: 8px;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
  color: var(--color-background);
}

.brand-text {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.auth-subtitle {
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  cursor: pointer;
}

.link {
  color: var(--color-text-primary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  transition: var(--transition-normal);
}

.link:hover {
  color: var(--color-text-secondary);
  text-decoration: underline;
}

.error-alert {
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.auth-footer {
  margin-top: var(--space-xl);
  text-align: center;
  font-size: var(--font-size-sm);
}

.footer-text {
  color: var(--color-text-muted);
  margin-right: var(--space-sm);
}

.demo-hint {
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--color-border);
  text-align: center;
}

.demo-hint p {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

@media (max-width: 480px) {
  .auth-card {
    padding: var(--space-xl);
  }
}
</style>
