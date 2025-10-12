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
  title: '智能体协同',
  description: '多工具调用与任务协同，处理复杂问题',
  placeholder: '描述你的复杂任务',
  loadingText: '智能体思考中...',
  theme: 'purple'
};
</script>