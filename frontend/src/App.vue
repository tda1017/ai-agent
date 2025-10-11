<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-content">
        <RouterLink class="brand" to="/">
          <div class="brand-icon">🤖</div>
          <span class="brand-text">AI Agent</span>
        </RouterLink>
        <nav class="nav">
          <RouterLink to="/apps" class="nav-link">AI 应用</RouterLink>
          <RouterLink to="/manus" class="nav-link">超级智能体</RouterLink>
        </nav>
      </div>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-surface-elevated);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: var(--shadow-sm);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-decoration: none;
  color: var(--color-text-primary);
  font-weight: 700;
  font-size: var(--font-size-lg);
  transition: var(--transition-normal);
}

.brand:hover {
  transform: translateY(-1px);
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: var(--radius-md);
  font-size: 16px;
}

.brand-text {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav {
  display: flex;
  gap: var(--space-lg);
}

.nav-link {
  position: relative;
  color: var(--color-text-secondary);
  text-decoration: none;
  font-weight: 500;
  font-size: var(--font-size-base);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  transition: var(--transition-normal);
}

.nav-link:hover {
  color: var(--color-text-primary);
  background: rgba(59, 130, 246, 0.05);
}

.nav-link.router-link-active {
  color: var(--color-primary);
  background: rgba(59, 130, 246, 0.1);
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: 1px;
}

.app-main {
  flex: 1;
  padding: var(--space-lg);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: var(--space-md);
    flex-wrap: wrap;
    gap: var(--space-md);
  }

  .nav {
    gap: var(--space-md);
  }

  .brand {
    font-size: var(--font-size-base);
  }

  .brand-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }

  .app-main {
    padding: var(--space-md);
  }
}

@media (max-width: 480px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .nav {
    width: 100%;
    justify-content: flex-start;
  }

  .nav-link {
    padding: var(--space-xs) var(--space-sm);
    font-size: var(--font-size-sm);
  }
}
</style>