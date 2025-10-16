import { nextTick, onBeforeUnmount, ref } from 'vue';
import { getMessages, sendMessage } from '../services/chatService';

/**
 * 统一聊天逻辑的组合式函数
 * 支持会话切换和历史消息加载
 */
export function useChat(streamFn) {
  const conversationId = ref(null);
  const messages = ref([]);
  const draft = ref('');
  const isStreaming = ref(false);
  const isLoadingHistory = ref(false);
  const messageWrap = ref(null);
  let stopStream = null;

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

  async function loadMessages(convId, lastId = 0) {
    try {
      isLoadingHistory.value = true;
      const data = await getMessages(convId, lastId);
      
      if (Array.isArray(data)) {
        const formattedMessages = data.map(msg => ({
          id: msg.id,
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content,
          timestamp: msg.createdAt
        }));
        
        if (lastId === 0) {
          messages.value = formattedMessages;
        } else {
          messages.value = [...formattedMessages, ...messages.value];
        }
      }
    } catch (error) {
      console.error('加载历史消息失败', error);
    } finally {
      isLoadingHistory.value = false;
      startScrollIntoView();
    }
  }

  async function switchConversation(convId) {
    if (convId === conversationId.value) return;
    
    conversationId.value = convId;
    messages.value = [];
    draft.value = '';
    
    if (convId) {
      await loadMessages(convId, 0);
    }
  }

  function startNewConversation() {
    conversationId.value = null;
    messages.value = [];
    draft.value = '';
  }

  async function handleSubmit() {
    const text = draft.value.trim();
    if (!text || isStreaming.value) {
      return;
    }

    const userMessage = {
      id: `${Date.now()}_user`,
      role: 'user',
      content: text,
      timestamp: new Date().toISOString()
    };
    messages.value.push(userMessage);
    draft.value = '';
    startScrollIntoView();

    try {
      const response = await sendMessage(conversationId.value, text);
      
      if (response.conversationId) {
        conversationId.value = response.conversationId;
      }

      const assistantMessage = {
        id: response.messageId || `${Date.now()}_assistant`,
        role: 'assistant',
        content: '',
        timestamp: new Date().toISOString()
      };
      messages.value.push(assistantMessage);

      isStreaming.value = true;
      stopStream?.();
      
      stopStream = streamFn(conversationId.value, text, {
        onChunk: (chunk) => {
          if (chunk?.type === 'start') {
            return;
          }
          if (chunk?.type === 'done' || chunk === 'done') {
            isStreaming.value = false;
            stopStream?.();
            stopStream = null;
            
            // 仅在未收到任何流式内容时，才回退到后端同步返回
            if (response.answer && !assistantMessage.content) {
              assistantMessage.content = response.answer;
            }
            startScrollIntoView();
            return;
          }
          appendChunk(assistantMessage, chunk);
          startScrollIntoView();
        },
        onComplete: () => {
          isStreaming.value = false;
          stopStream = null;
          
          if (response.answer && !assistantMessage.content) {
            assistantMessage.content = response.answer;
          }
          startScrollIntoView();
        },
        onError: (error) => {
          console.error('SSE 发生错误', error);
          isStreaming.value = false;
          stopStream = null;
          
          if (response.answer && !assistantMessage.content) {
            assistantMessage.content = response.answer;
          }
        }
      });
    } catch (error) {
      console.error('发送消息失败', error);
      messages.value.push({
        id: `${Date.now()}_error`,
        role: 'assistant',
        content: '发送失败，请重试',
        timestamp: new Date().toISOString()
      });
    }
  }

  onBeforeUnmount(() => {
    stopStream?.();
  });

  return {
    conversationId,
    messages,
    draft,
    isStreaming,
    isLoadingHistory,
    messageWrap,
    handleSubmit,
    switchConversation,
    startNewConversation,
    loadMessages
  };
}