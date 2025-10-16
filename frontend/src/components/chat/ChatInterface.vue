<template>
  <div class="chat-page">
    <ConversationList
      :conversations="conversations"
      :selected-id="conversationId"
      :loading="loadingConversations"
      :error="conversationsError"
      @select="handleSelectConversation"
      @delete="handleDeleteConversation"
      @new-conversation="handleNewConversation"
      @reload="loadConversationList"
    />
    
    <div class="chat-main">
      <section class="chat-header" :class="themeClass">
        <div class="header-content">
          <div class="title-section">
            <h1>{{ config.title }}</h1>
            <span v-if="conversationId" class="session-id">会话 #{{ conversationId }}</span>
            <span v-else class="session-id">新对话</span>
          </div>
        </div>
      </section>

    <section class="chat-window" :class="themeClass">
      <div ref="messageWrap" class="messages">
        <div v-if="messages.length === 0" class="welcome-message">
          <h3>开始对话</h3>
          <p>{{ config.description || '有什么可以帮助你？' }}</p>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useChat } from '../../composables/useChat.js';
import { getConversations, deleteConversation, createConversation } from '../../services/chatService.js';
import ConversationList from './ConversationList.vue';

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
  }
});

const themeClass = computed(() => `theme-${props.config.theme}`);

const {
  conversationId,
  messages,
  draft,
  isStreaming,
  messageWrap,
  handleSubmit,
  switchConversation,
  startNewConversation
} = useChat(props.streamFunction);

const conversations = ref([]);
const loadingConversations = ref(false);
const conversationsError = ref(null);

async function loadConversationList() {
  try {
    loadingConversations.value = true;
    conversationsError.value = null;
    const data = await getConversations(20);
    
    if (Array.isArray(data)) {
      conversations.value = data.map(conv => ({
        id: conv.id,
        title: conv.title || '新对话',
        updatedAt: conv.updatedAt
      }));
    }
  } catch (error) {
    console.error('加载会话列表失败', error);
    conversationsError.value = '加载失败';
  } finally {
    loadingConversations.value = false;
  }
}

async function handleSelectConversation(convId) {
  await switchConversation(convId);
}

async function handleDeleteConversation(convId) {
  try {
    await deleteConversation(convId);
    conversations.value = conversations.value.filter(c => c.id !== convId);
    
    if (convId === conversationId.value) {
      startNewConversation();
    }
  } catch (error) {
    console.error('删除会话失败', error);
    alert('删除失败，请重试');
  }
}

function handleNewConversation() {
  // 立即在后端创建会话，拿到 id 后选中
  createConversation()
    .then((data) => {
      const id = data?.id
      // 列表添加并置顶
      conversations.value = [{ id, title: data?.title || '新对话', updatedAt: new Date().toISOString() }, ...conversations.value]
      switchConversation(id)
    })
    .catch((e) => {
      console.error('创建会话失败', e)
      // 回退到纯前端新会话
      startNewConversation()
    })
}

onMounted(() => {
  loadConversationList();
});

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
  flex-direction: row;
  height: 100%;
  flex: 1;
  /* 统一聊天区域最大宽度，可按需调整 */
  --chat-max-width: 1100px;
}

.chat-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* Chat Header */
.chat-header {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-lg) var(--space-xl);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: var(--chat-max-width);
  width: 100%;
  margin: 0 auto;
}

.title-section {
  display: flex;
  align-items: baseline;
  gap: var(--space-md);
}

.title-section h1 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.session-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-family: 'Monaco', 'Consolas', monospace;
}

/* Chat Window */
.chat-window {
  flex: 1;
  min-height: 600px;
  background: var(--color-background);
  display: flex;
  flex-direction: column;
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
  max-width: var(--chat-max-width);
  width: 100%;
  margin: 0 auto;
}

.welcome-message {
  text-align: center;
  padding: var(--space-3xl) var(--space-xl);
  color: var(--color-text-muted);
}

.welcome-message h3 {
  font-size: var(--font-size-base);
  font-weight: 400;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-sm);
}

.welcome-message p {
  font-size: var(--font-size-sm);
  margin: 0;
}

.message {
  display: flex;
  gap: var(--space-md);
  max-width: 100%;
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
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: var(--font-size-xs);
  background: var(--color-surface-elevated);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.avatar.user {
  background: var(--color-text-primary);
  color: var(--color-background);
  border-color: var(--color-text-primary);
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
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  line-height: 1.5;
  word-wrap: break-word;
  font-size: var(--font-size-sm);
}

.bubble.user {
  background: var(--color-text-primary);
  color: var(--color-background);
}

.bubble.assistant {
  background: var(--color-surface-elevated);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.bubble.typing {
  background: var(--color-surface-elevated);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
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
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  max-width: var(--chat-max-width);
  width: 100%;
  margin: 0 auto;
}

.input-container {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: var(--space-sm);
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-sm) var(--space-md);
  transition: var(--transition-normal);
}

.input-container:focus-within {
  border-color: var(--color-text-primary);
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
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--color-text-primary);
  color: var(--color-background);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition-normal);
  flex-shrink: 0;
}

.send-btn:not(.disabled):hover {
  opacity: 0.8;
}

.send-btn.disabled {
  background: var(--color-surface-hover);
  color: var(--color-text-muted);
  cursor: not-allowed;
  opacity: 0.5;
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
    max-width: 100%;
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
    max-width: 100%;
    gap: var(--space-sm);
  }

  .avatar {
    width: 32px;
    height: 32px;
    font-size: var(--font-size-xs);
  }
}
</style>