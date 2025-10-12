<template>
  <div class="app-shell">
    <header class="app-header">
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
            <span>AI 应用</span>
          </RouterLink>
          <RouterLink to="/manus" class="nav-link">
            <span>超级智能体</span>
          </RouterLink>
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
  background: var(--color-background);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px) saturate(180%);
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
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: var(--radius-md);
  padding: 6px;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
  color: #0f172a;
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

.app-main {
  flex: 1;
  padding: var(--space-xl);
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