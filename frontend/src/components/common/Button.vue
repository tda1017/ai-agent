<template>
  <button 
    :class="['btn', `btn-${variant}`, { 'btn-loading': loading, 'btn-block': block }]"
    :disabled="disabled || loading"
    :type="type"
    @click="handleClick"
  >
    <span v-if="loading" class="btn-spinner"></span>
    <slot v-else />
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'outline', 'ghost', 'danger'].includes(value)
  },
  type: {
    type: String,
    default: 'button'
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  emit('click', event)
}
</script>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 500;
  transition: var(--transition-normal);
  white-space: nowrap;
  min-height: 40px;
}

.btn-primary {
  background: var(--color-text-primary);
  color: var(--color-background);
  border: 1px solid var(--color-text-primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-text-secondary);
  border-color: var(--color-text-secondary);
}

.btn-secondary {
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-surface-elevated);
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
}

.btn-outline:hover:not(:disabled) {
  background: var(--color-surface-elevated);
  border-color: var(--color-text-primary);
}

.btn-ghost {
  background: transparent;
  color: var(--color-text-muted);
}

.btn-ghost:hover:not(:disabled) {
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.btn-danger {
  background: var(--color-surface-hover);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.btn-danger:hover:not(:disabled) {
  background: var(--color-surface-elevated);
  border-color: var(--color-text-muted);
}

.btn-block {
  width: 100%;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-loading {
  pointer-events: none;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn:active:not(:disabled) {
  transform: translateY(0);
}
</style>
