<template>
  <div class="home">
    <section class="hero">
      <div class="hero-content">
        <div class="hero-badge">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <circle cx="6" cy="6" r="6" fill="currentColor"/>
          </svg>
          <span>新一代 AI 平台</span>
        </div>
        <h1 class="hero-title">DeepSeek AI 助手<br/>智能对话平台</h1>
        <p class="hero-description">基于 DeepSeek 模型，提供专业、准确的 AI 对话服务</p>
      </div>
    </section>

    <section class="features">
      <div class="cards-grid">
        <RouterLink class="feature-card" to="/apps">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>DeepSeek 对话</h3>
            <p>通用 AI 助手，支持各类问题解答</p>
          </div>
          <div class="card-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </div>
        </RouterLink>

        <RouterLink class="feature-card" to="/manus">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>智能体协同</h3>
            <p>多工具调用与任务协同，处理复杂问题</p>
          </div>
          <div class="card-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </div>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-3xl) 0;
}

/* Hero Section */
.hero {
  margin-bottom: var(--space-3xl);
}

.hero-content {
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 100px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 500;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-bottom: var(--space-xl);
}

.hero-badge svg {
  color: var(--color-text-primary);
}

.hero-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  line-height: 1.1;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-lg);
  letter-spacing: -0.03em;
}

.hero-description {
  font-size: var(--font-size-lg);
  color: var(--color-text-muted);
  line-height: 1.6;
  margin: 0;
  max-width: 600px;
  margin: 0 auto;
}

/* Features Section */
.features {
  margin-top: var(--space-2xl);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-md);
}

.feature-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  transition: var(--transition-normal);
  position: relative;
}

.feature-card:hover {
  background: var(--color-surface-elevated);
  border-color: var(--color-text-primary);
}

.feature-card:hover .card-arrow {
  transform: translateX(4px);
}

.card-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.card-icon svg {
  width: 20px;
  height: 20px;
  color: var(--color-text-primary);
}

.card-content {
  flex: 1;
}

.card-content h3 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
  letter-spacing: -0.01em;
}

.card-content p {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.5;
}

.card-arrow {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  opacity: 0.4;
  transition: var(--transition-normal);
}

.card-arrow svg {
  width: 100%;
  height: 100%;
  color: var(--color-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home {
    padding: var(--space-2xl) 0;
  }

  .hero-title {
    font-size: var(--font-size-2xl);
  }

  .hero-description {
    font-size: var(--font-size-base);
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }

  .feature-card {
    padding: var(--space-lg);
  }
}

@media (max-width: 480px) {
  .home {
    padding: var(--space-xl) 0;
  }

  .hero-title {
    font-size: var(--font-size-xl);
  }

  .feature-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-arrow {
    align-self: flex-end;
  }
}
</style>