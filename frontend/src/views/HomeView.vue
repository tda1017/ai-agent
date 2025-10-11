<template>
  <div class="home">
    <section class="hero">
      <div class="hero-content">
        <div class="hero-badge">🚀 新一代 AI 工作台</div>
        <h1 class="hero-title">AI Agent <span class="gradient-text">智能控制台</span></h1>
        <p class="hero-description">体验最先进的AI对话和智能体协同能力，让AI成为你的超级助手。</p>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-number">3+</div>
            <div class="stat-label">AI 模式</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">∞</div>
            <div class="stat-label">对话能力</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">24/7</div>
            <div class="stat-label">在线服务</div>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="floating-card card-1">
          <div class="card-icon">🤖</div>
          <div class="card-text">智能对话</div>
        </div>
        <div class="floating-card card-2">
          <div class="card-icon">⚡</div>
          <div class="card-text">实时流式</div>
        </div>
        <div class="floating-card card-3">
          <div class="card-icon">🔗</div>
          <div class="card-text">工具调用</div>
        </div>
      </div>
    </section>

    <section class="features">
      <h2 class="section-title">选择你的 AI 体验</h2>
      <div class="cards-grid">
        <RouterLink class="feature-card primary" to="/apps">
          <div class="card-header">
            <div class="card-icon-large">💬</div>
            <div class="card-badge">热门</div>
          </div>
          <div class="card-body">
            <h3>AI 对话助手</h3>
            <p>支持实时流式对话，智能上下文理解，让对话更自然流畅。</p>
            <ul class="feature-list">
              <li>实时 SSE 流式输出</li>
              <li>上下文记忆功能</li>
              <li>多轮对话支持</li>
            </ul>
          </div>
          <div class="card-footer">
            <span class="cta-text">立即体验 →</span>
          </div>
        </RouterLink>

        <RouterLink class="feature-card secondary" to="/manus">
          <div class="card-header">
            <div class="card-icon-large">🧠</div>
            <div class="card-badge new">NEW</div>
          </div>
          <div class="card-body">
            <h3>超级智能体</h3>
            <p>多智能体协同工作，可视化推理过程，解决复杂任务。</p>
            <ul class="feature-list">
              <li>多智能体协同</li>
              <li>推理过程可视化</li>
              <li>复杂任务分解</li>
            </ul>
          </div>
          <div class="card-footer">
            <span class="cta-text">探索更多 →</span>
          </div>
        </RouterLink>
      </div>
    </section>

    <section class="info-banner">
      <div class="banner-content">
        <div class="banner-icon">ℹ️</div>
        <div class="banner-text">
          <strong>开发说明：</strong>当前为前端演示版本，后端服务就绪后将自动连接真实AI接口。
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: var(--space-2xl);
  max-width: 1200px;
  margin: 0 auto;
}

/* Hero Section */
.hero {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: var(--space-2xl);
  align-items: center;
  padding: var(--space-2xl) 0;
  position: relative;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.hero-badge {
  display: inline-block;
  padding: var(--space-xs) var(--space-md);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(139, 92, 246, 0.1));
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: var(--radius-xl);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 600;
  width: fit-content;
}

.hero-title {
  font-size: var(--font-size-3xl);
  font-weight: 800;
  line-height: 1.2;
  color: var(--color-text-primary);
  margin: 0;
}

.gradient-text {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
  max-width: 500px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-top: var(--space-md);
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: var(--space-xs);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(0, 0, 0, 0.1);
}

.hero-visual {
  position: relative;
  height: 300px;
}

.floating-card {
  position: absolute;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  box-shadow: var(--shadow-lg);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  animation: float 6s ease-in-out infinite;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.floating-card .card-icon {
  font-size: 20px;
}

.floating-card .card-text {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-1 {
  top: 20px;
  right: 50px;
  animation-delay: -2s;
}

.card-2 {
  top: 120px;
  right: 20px;
  animation-delay: -4s;
}

.card-3 {
  top: 220px;
  right: 80px;
  animation-delay: -6s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* Features Section */
.section-title {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  text-align: center;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2xl);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: var(--space-xl);
}

.feature-card {
  display: flex;
  flex-direction: column;
  padding: var(--space-xl);
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  text-decoration: none;
  color: inherit;
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  transform: scaleX(0);
  transform-origin: left;
  transition: var(--transition-normal);
}

.feature-card:hover::before {
  transform: scaleX(1);
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-primary);
}

.feature-card.primary {
  box-shadow: var(--shadow-md);
}

.feature-card.secondary {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-lg);
}

.card-icon-large {
  font-size: 48px;
  margin-bottom: var(--space-sm);
}

.card-badge {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
}

.card-badge:is(.hot) {
  background: linear-gradient(135deg, #ff6b6b, #ffa500);
  color: white;
}

.card-badge.new {
  background: linear-gradient(135deg, var(--color-secondary), var(--color-accent));
  color: white;
}

.card-body h3 {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-md);
}

.card-body p {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 var(--space-lg);
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.feature-list li::before {
  content: '✓';
  color: var(--color-primary);
  font-weight: 600;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 50%;
  font-size: 10px;
}

.card-footer {
  margin-top: auto;
  padding-top: var(--space-lg);
}

.cta-text {
  color: var(--color-primary);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

/* Info Banner */
.info-banner {
  padding: var(--space-lg);
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.05), rgba(59, 130, 246, 0.05));
  border: 1px solid rgba(6, 182, 212, 0.2);
  border-radius: var(--radius-lg);
}

.banner-content {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  max-width: 800px;
  margin: 0 auto;
}

.banner-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.banner-text {
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.banner-text strong {
  color: var(--color-text-primary);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .hero {
    grid-template-columns: 1fr;
    gap: var(--space-xl);
    text-align: center;
  }

  .hero-visual {
    display: none;
  }

  .cards-grid {
    grid-template-columns: 1fr;
    gap: var(--space-lg);
  }
}

@media (max-width: 768px) {
  .hero {
    padding: var(--space-xl) 0;
  }

  .hero-title {
    font-size: var(--font-size-2xl);
  }

  .hero-stats {
    justify-content: center;
  }

  .feature-card {
    padding: var(--space-lg);
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
    gap: var(--space-sm);
  }
}

@media (max-width: 480px) {
  .home {
    gap: var(--space-xl);
  }

  .hero-description {
    font-size: var(--font-size-base);
  }

  .hero-stats {
    flex-wrap: wrap;
    gap: var(--space-md);
  }

  .stat-divider {
    display: none;
  }
}
</style>