<template>
  <div class="chat-page">
    <section class="chat-header" :class="themeClass">
      <div class="header-content">
        <div class="title-section">
          <div class="chat-icon">💬</div>
          <div class="title-text">
            <h1>{{ config.title }}</h1>
            <p>{{ config.description }}</p>
          </div>
        </div>
        <div class="header-meta">
          <div class="session-badge">
            <span class="session-label">会话</span>
            <span class="session-id">{{ sessionId }}</span>
          </div>
          <div class="status-indicator" :class="{ active: !isStreaming }">
            <span class="status-dot"></span>
            <span class="status-text">{{ isStreaming ? '思考中' : '就绪' }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="chat-window" :class="themeClass">
      <div ref="messageWrap" class="messages">
        <div v-if="messages.length === 0" class="welcome-message">
          <div class="welcome-icon">🤖</div>
          <h3>开始对话吧！</h3>
          <p>我是你的AI助手，有什么可以帮助你的吗？</p>
        </div>
        <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="message.role"
        >
          <div class="message-avatar">
            <div class="avatar" :class="[message.role, themeClass]">
              {{ message.role === 'user' ? '你' : 'AI' }}
            </div>
          </div>
          <div class="message-content">
            <div class="bubble" :class="[message.role, themeClass]">
              {{ message.content }}
            </div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>
        <div v-if="isStreaming" class="message assistant typing-message">
          <div class="message-avatar">
            <div class="avatar assistant" :class="themeClass">
              <div class="thinking-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
          <div class="message-content">
            <div class="bubble typing" :class="themeClass">{{ config.loadingText }}</div>
          </div>
        </div>
      </div>

      <form class="composer" @submit.prevent="handleSubmit">
        <div class="input-container">
          <textarea
            v-model="draft"
            class="input"
            :placeholder="config.placeholder"
            :disabled="isStreaming"
            @keydown.enter.exact.prevent="handleSubmit"
            rows="1"
          ></textarea>
          <button
            class="send-btn"
            :class="[themeClass, { disabled: isStreaming || !draft.trim() }]"
            type="submit"
            :disabled="isStreaming || !draft.trim()"
          >
            <svg v-if="!isStreaming" class="send-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="m22 2-7 20-4-9-9-4z"/>
              <path d="M22 2 11 13"/>
            </svg>
            <div v-else class="loading-spinner"></div>
          </button>
        </div>
        <div class="composer-footer">
          <span class="tip-text">按 Enter 发送，Shift + Enter 换行</span>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useChat } from '../../composables/useChat.js';

const props = defineProps({
  config: {
    type: Object,
    required: true,
    default: () => ({
      title: 'AI 聊天',
      description: '智能对话助手',
      placeholder: '请输入你的问题',
      loadingText: '回应生成中...',
      theme: 'blue'
    })
  },
  streamFunction: {
    type: Function,
    required: true
  },
  sendFunction: {
    type: Function,
    required: true
  }
});

const themeClass = computed(() => `theme-${props.config.theme}`);

const {
  sessionId,
  messages,
  draft,
  isStreaming,
  messageWrap,
  handleSubmit
} = useChat(props.streamFunction, props.sendFunction);

// 格式化时间显示
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
};
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  max-width: 1000px;
  margin: 0 auto;
}

/* Chat Header */
.chat-header {
  background: var(--color-surface-elevated);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-lg);
}

.title-section {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.chat-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.title-text h1 {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

.title-text p {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.session-badge {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: rgba(59, 130, 246, 0.1);
  border-radius: var(--radius-md);
}

.session-label {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  font-weight: 600;
  text-transform: uppercase;
}

.session-id {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-family: 'Monaco', 'Consolas', monospace;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.status-dot {
  width: 8px;
  height: 8px;
  background: var(--color-text-muted);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.active .status-dot {
  background: #10b981;
}

.status-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Chat Window */
.chat-window {
  flex: 1;
  min-height: 600px;
  background: var(--color-surface-elevated);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-md);
  border: 1px solid rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* Messages */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.welcome-message {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-secondary);
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: var(--space-md);
}

.welcome-message h3 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
}

.welcome-message p {
  margin: 0;
  max-width: 300px;
  margin: 0 auto;
}

.message {
  display: flex;
  gap: var(--space-md);
  max-width: 85%;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message.assistant {
  margin-right: auto;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: var(--font-size-sm);
  color: white;
}

.avatar.user {
  background: var(--color-primary);
}

.avatar.assistant {
  background: var(--color-text-secondary);
}

.avatar.theme-purple.user {
  background: #db2777;
}

.avatar.theme-purple.assistant {
  background: #7c3aed;
}

.thinking-dots {
  display: flex;
  gap: 2px;
}

.thinking-dots span {
  width: 4px;
  height: 4px;
  background: currentColor;
  border-radius: 50%;
  animation: thinking 1.4s ease-in-out infinite both;
}

.thinking-dots span:nth-child(1) { animation-delay: -0.32s; }
.thinking-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes thinking {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.message-content {
  flex: 1;
  min-width: 0;
}

.bubble {
  padding: var(--space-md) var(--space-lg);
  border-radius: var(--radius-lg);
  line-height: 1.6;
  word-wrap: break-word;
  position: relative;
}

.bubble.user {
  background: var(--color-primary);
  color: white;
  border-bottom-right-radius: var(--space-sm);
}

.bubble.assistant {
  background: var(--color-background);
  color: var(--color-text-primary);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-bottom-left-radius: var(--space-sm);
}

.bubble.typing {
  background: var(--color-background);
  color: var(--color-text-muted);
  font-style: italic;
  animation: pulse 2s infinite;
}

.bubble.theme-purple.user {
  background: #db2777;
}

.bubble.theme-purple.assistant {
  border-color: rgba(139, 92, 246, 0.2);
}

.message-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: var(--space-xs);
  text-align: right;
}

.message.user .message-time {
  text-align: left;
}

/* Composer */
.composer {
  padding: var(--space-lg);
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: var(--color-surface);
}

.input-container {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: var(--space-md);
  background: var(--color-surface-elevated);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  transition: var(--transition-normal);
}

.input-container:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  resize: none;
  max-height: 120px;
  line-height: 1.5;
}

.input::placeholder {
  color: var(--color-text-muted);
}

.input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition-normal);
  flex-shrink: 0;
}

.send-btn:not(.disabled):hover {
  background: var(--color-primary-dark);
  transform: scale(1.05);
}

.send-btn.disabled {
  background: var(--color-text-muted);
  cursor: not-allowed;
  transform: none;
}

.send-btn.theme-purple:not(.disabled) {
  background: #7c3aed;
}

.send-btn.theme-purple:not(.disabled):hover {
  background: #6d28d9;
}

.send-icon {
  width: 18px;
  height: 18px;
  stroke-width: 2;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.composer-footer {
  margin-top: var(--space-sm);
  text-align: center;
}

.tip-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-page {
    margin: 0 var(--space-md);
  }

  .chat-header {
    padding: var(--space-lg);
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-md);
  }

  .header-meta {
    width: 100%;
    justify-content: space-between;
  }

  .chat-window {
    min-height: 500px;
  }

  .messages {
    padding: var(--space-md);
  }

  .message {
    max-width: 90%;
  }

  .composer {
    padding: var(--space-md);
  }
}

@media (max-width: 480px) {
  .chat-page {
    margin: 0;
    gap: var(--space-sm);
  }

  .chat-header {
    border-radius: 0;
    padding: var(--space-md);
  }

  .chat-window {
    border-radius: 0;
    min-height: calc(100vh - 200px);
  }

  .title-section {
    gap: var(--space-sm);
  }

  .chat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .title-text h1 {
    font-size: var(--font-size-lg);
  }

  .header-meta {
    flex-wrap: wrap;
    gap: var(--space-sm);
  }

  .message {
    max-width: 95%;
    gap: var(--space-sm);
  }

  .avatar {
    width: 32px;
    height: 32px;
    font-size: var(--font-size-xs);
  }
}
</style>