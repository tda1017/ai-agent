<template>
  <div class="app-shell">
    <header v-if="!hideNav" class="app-header">
      <div class="header-content">
        <RouterLink class="brand" to="/">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </div>
          <span class="brand-text">AI Agent</span>
        </RouterLink>
        <nav class="nav">
          <RouterLink to="/apps" class="nav-link">
            <span>DeepSeek 对话</span>
          </RouterLink>
          <RouterLink to="/manus" class="nav-link">
            <span>智能体协同</span>
          </RouterLink>
          <div v-if="isAuthenticated" class="user-menu-wrapper">
            <button class="user-menu-trigger" @click="toggleUserMenu">
              <div class="user-avatar">
                <span>{{ userInitial }}</span>
              </div>
              <svg class="chevron" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
              </svg>
            </button>
            <div v-if="showUserMenu" class="user-menu">
              <div class="user-menu-header">
                <div class="user-info">
                  <div class="user-name">{{ user?.username }}</div>
                  <div class="user-email">{{ user?.email }}</div>
                </div>
              </div>
              <div class="user-menu-divider"></div>
              <button class="user-menu-item" @click="handleLogout">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd" />
                </svg>
                退出登录
              </button>
            </div>
          </div>
        </nav>
      </div>
    </header>
    <main class="app-main" :class="{ 'no-header': hideNav }">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const { user, isAuthenticated, logout, initAuth } = useAuth()

const showUserMenu = ref(false)

const hideNav = computed(() => route.meta.hideNav)
const userInitial = computed(() => user.value?.username?.[0]?.toUpperCase() || 'U')

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const handleLogout = () => {
  showUserMenu.value = false
  logout()
}

onMounted(() => {
  initAuth()
  
  document.addEventListener('click', (e) => {
    const userMenuWrapper = document.querySelector('.user-menu-wrapper')
    if (userMenuWrapper && !userMenuWrapper.contains(e.target)) {
      showUserMenu.value = false
    }
  })
})

watch(() => route.path, () => {
  showUserMenu.value = false
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(10, 10, 10, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--color-border);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-xl);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  text-decoration: none;
  color: var(--color-text-primary);
  font-weight: 700;
  font-size: var(--font-size-lg);
  transition: var(--transition-normal);
}

.brand:hover {
  opacity: 0.8;
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-text-primary);
  border-radius: var(--radius-sm);
  padding: 6px;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
  color: var(--color-background);
}

.brand-text {
  letter-spacing: -0.02em;
}

.nav {
  display: flex;
  gap: var(--space-xs);
}

.nav-link {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
  font-weight: 500;
  font-size: var(--font-size-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  transition: var(--transition-normal);
}

.nav-link span {
  position: relative;
  z-index: 1;
}

.nav-link:hover {
  color: var(--color-text-primary);
  background: var(--color-surface);
}

.nav-link.router-link-active {
  color: var(--color-text-primary);
  background: var(--color-surface);
}

.user-menu-wrapper {
  position: relative;
  margin-left: var(--space-md);
}

.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs);
  background: transparent;
  border-radius: var(--radius-md);
  transition: var(--transition-normal);
}

.user-menu-trigger:hover {
  background: var(--color-surface);
}

.user-avatar {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-text-primary);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--color-background);
}

.chevron {
  width: 16px;
  height: 16px;
  color: var(--color-text-muted);
  transition: var(--transition-normal);
}

.user-menu-trigger:hover .chevron {
  color: var(--color-text-primary);
}

.user-menu {
  position: absolute;
  top: calc(100% + var(--space-sm));
  right: 0;
  min-width: 200px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-menu-header {
  padding: var(--space-md);
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.user-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
}

.user-email {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.user-menu-divider {
  height: 1px;
  background: var(--color-border);
}

.user-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: left;
  transition: var(--transition-normal);
}

.user-menu-item:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-primary);
}

.user-menu-item svg {
  width: 16px;
  height: 16px;
}

.app-main {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.app-main.no-header {
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: var(--space-md) var(--space-lg);
  }

  .nav {
    gap: var(--space-xs);
  }

  .brand {
    font-size: var(--font-size-base);
  }

  .brand-icon {
    width: 32px;
    height: 32px;
  }

  .app-main {
    padding: var(--space-lg);
  }

  .nav-link {
    padding: var(--space-xs) var(--space-sm);
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: var(--space-md);
  }

  .app-main {
    padding: var(--space-md);
  }
}
</style>