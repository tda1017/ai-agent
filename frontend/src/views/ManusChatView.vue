<template>
  <ChatInterface
    :config="chatConfig"
    :stream-function="streamManusChat"
    :send-function="sendManusPrompt"
  />
</template>

<script setup>
import ChatInterface from '../components/chat/ChatInterface.vue';
import { sendManusPrompt, streamManusChat } from '../services/chatService';

const chatConfig = {
  title: 'AI 超级智能体',
  description: '联合多工具的复杂任务调度入口，暂以模拟数据演示流式回复效果。',
  placeholder: '描述你的复杂目标，回车发送',
  loadingText: '智能体推理中...',
  theme: 'purple'
};
</script>