<template>
  <div class="conversation-list">
    <div class="list-header">
      <h3>对话历史</h3>
      <button class="new-chat-btn" @click="$emit('new-conversation')" title="新建对话">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
      </button>
    </div>

    <div class="list-content">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <button @click="$emit('reload')">重试</button>
      </div>

      <div v-else-if="conversations.length === 0" class="empty-state">
        <p>暂无对话记录</p>
        <span>开始新对话吧</span>
      </div>

      <div v-else class="conversations">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === selectedId }"
          @click="$emit('select', conv.id)"
        >
          <div class="conversation-content">
            <div class="conversation-title">{{ formatTitle(conv.title) }}</div>
            <div class="conversation-time">{{ formatTime(conv.updatedAt) }}</div>
          </div>
          <button
            class="delete-btn"
            @click.stop="handleDelete(conv.id)"
            title="删除对话"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  conversations: {
    type: Array,
    default: () => []
  },
  selectedId: {
    type: Number,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  }
});

const emit = defineEmits(['select', 'delete', 'new-conversation', 'reload']);

const formatTitle = (title) => {
  if (!title) return '新对话';
  return title.length > 40 ? title.substring(0, 40) + '...' : title;
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000);
    return `${days}天前`;
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
};

const handleDelete = (id) => {
  if (confirm('确定删除这条对话记录吗？')) {
    emit('delete', id);
  }
};
</script>

<style scoped>
.conversation-list {
  width: 280px;
  min-width: 280px;
  max-width: 280px;
  flex-shrink: 0;
  height: 100%;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: var(--space-lg);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.new-chat-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-normal);
}

.new-chat-btn:hover {
  background: var(--color-surface-hover);
  border-color: var(--color-text-primary);
  color: var(--color-text-primary);
}

.new-chat-btn svg {
  width: 18px;
  height: 18px;
}

.list-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  gap: var(--space-md);
  color: var(--color-text-muted);
  text-align: center;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-text-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state p {
  color: var(--color-error);
  margin: 0;
}

.error-state button {
  padding: var(--space-sm) var(--space-md);
  background: var(--color-text-primary);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
}

.empty-state p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.empty-state span {
  font-size: var(--font-size-xs);
}

.conversations {
  padding: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-normal);
  border: 1px solid transparent;
}

.conversation-item:hover {
  background: var(--color-surface-hover);
}

.conversation-item.active {
  background: var(--color-surface-elevated);
  border-color: var(--color-border);
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: var(--space-xs);
}

.conversation-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.delete-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition-normal);
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: var(--color-error);
  color: white;
}

.delete-btn svg {
  width: 16px;
  height: 16px;
}

@media (max-width: 768px) {
  .conversation-list {
    width: 280px;
    min-width: 280px;
    max-width: 280px;
    flex-shrink: 0;
  }
}
</style>
