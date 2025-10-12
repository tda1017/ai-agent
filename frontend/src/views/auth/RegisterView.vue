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
        <p class="auth-subtitle">创建新账号</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <Input
          v-model="form.username"
          label="用户名"
          type="text"
          placeholder="请输入用户名"
          required
          autocomplete="username"
          :error="errors.username"
          hint="3-20个字符，支持字母、数字、下划线"
        />

        <Input
          v-model="form.email"
          label="邮箱"
          type="email"
          placeholder="请输入邮箱地址"
          required
          autocomplete="email"
          :error="errors.email"
        />

        <Input
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          required
          autocomplete="new-password"
          :error="errors.password"
          hint="至少8个字符"
        />

        <Input
          v-model="form.confirmPassword"
          label="确认密码"
          type="password"
          placeholder="请再次输入密码"
          required
          autocomplete="new-password"
          :error="errors.confirmPassword"
        />

        <label class="checkbox-label">
          <input type="checkbox" v-model="form.agree" required />
          <span>我已阅读并同意 <a href="#" class="link">服务条款</a> 和 <a href="#" class="link">隐私政策</a></span>
        </label>

        <div v-if="error" class="error-alert">
          {{ error }}
        </div>

        <Button type="submit" :loading="loading" block>
          注册
        </Button>
      </form>

      <div class="auth-footer">
        <span class="footer-text">已有账号？</span>
        <RouterLink to="/auth/login" class="link">立即登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'

const { register, loading, error } = useAuth()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateForm = () => {
  let isValid = true
  errors.username = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''

  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    isValid = false
  } else if (form.username.length < 3 || form.username.length > 20) {
    errors.username = '用户名长度为3-20个字符'
    isValid = false
  } else if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
    errors.username = '用户名只能包含字母、数字、下划线'
    isValid = false
  }

  if (!form.email.trim()) {
    errors.email = '请输入邮箱'
    isValid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '请输入有效的邮箱地址'
    isValid = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (form.password.length < 8) {
    errors.password = '密码至少8个字符'
    isValid = false
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
    isValid = false
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }

  if (!form.agree) {
    isValid = false
  }

  return isValid
}

const handleRegister = async () => {
  if (!validateForm()) return

  try {
    await register({
      username: form.username,
      email: form.email,
      password: form.password
    })
  } catch (e) {
    console.error('Register failed:', e)
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
  max-width: 420px;
  padding: var(--space-2xl);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
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

.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  line-height: 1.5;
}

.checkbox-label input[type="checkbox"] {
  margin-top: 4px;
  cursor: pointer;
}

.link {
  color: var(--color-text-primary);
  text-decoration: none;
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

@media (max-width: 480px) {
  .auth-card {
    padding: var(--space-xl);
  }
}
</style>
