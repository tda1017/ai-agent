<template>
  <ChatInterface
    :config="chatConfig"
    :stream-function="streamAppChat"
  />
</template>

<script setup>
import ChatInterface from '../components/chat/ChatInterface.vue';
import { streamAppChat } from '../services/chatService';

const chatConfig = {
  title: 'DeepSeek 助手',
  description: '基于 DeepSeek 模型的通用 AI 对话助手',
  placeholder: '有什么可以帮助你？',
  loadingText: '思考中...',
  theme: 'blue'
};
</script>