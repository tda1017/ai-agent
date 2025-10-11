import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

/**
 * 统一聊天逻辑的组合式函数
 * 消除AppChatView和ManusChatView的重复代码
 */
export function useChat(streamFn, sendFn) {
  const sessionId = ref('');
  const messages = ref([]);
  const draft = ref('');
  const isStreaming = ref(false);
  const messageWrap = ref(null);
  let stopStream = null;

  function generateSessionId() {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return `session_${Date.now()}_${Math.random().toString(16).slice(2, 10)}`;
  }

  function pushAssistantMessage() {
    const message = {
      id: `${Date.now()}_assistant`,
      role: 'assistant',
      content: ''
    };
    messages.value.push(message);
    return message;
  }

  function appendChunk(message, chunk) {
    if (!chunk) {
      return;
    }
    if (typeof chunk === 'string') {
      if (chunk === 'done') {
        return;
      }
      message.content += chunk;
      return;
    }
    if (chunk.type === 'delta' && chunk.content) {
      message.content += chunk.content;
    }
  }

  function startScrollIntoView() {
    nextTick(() => {
      const container = messageWrap.value;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    });
  }

  async function handleSubmit() {
    const text = draft.value.trim();
    if (!text || isStreaming.value) {
      return;
    }

    // 添加用户消息
    messages.value.push({
      id: `${Date.now()}_user`,
      role: 'user',
      content: text
    });
    draft.value = '';
    startScrollIntoView();

    // 发送消息到后端
    await sendFn(sessionId.value, text);

    // 开始流式接收
    const assistantMessage = pushAssistantMessage();
    isStreaming.value = true;

    stopStream?.();
    stopStream = streamFn(sessionId.value, text, {
      onChunk: (chunk) => {
        if (chunk?.type === 'start') {
          return;
        }
        if (chunk?.type === 'done' || chunk === 'done') {
          isStreaming.value = false;
          stopStream?.();
          stopStream = null;
          startScrollIntoView();
          return;
        }
        appendChunk(assistantMessage, chunk);
        startScrollIntoView();
      },
      onComplete: () => {
        isStreaming.value = false;
        stopStream = null;
        startScrollIntoView();
      },
      onError: (error) => {
        console.error('SSE 发生错误', error);
        isStreaming.value = false;
        stopStream = null;
      }
    });
  }

  onMounted(() => {
    sessionId.value = generateSessionId();
  });

  onBeforeUnmount(() => {
    stopStream?.();
  });

  return {
    sessionId,
    messages,
    draft,
    isStreaming,
    messageWrap,
    handleSubmit
  };
}