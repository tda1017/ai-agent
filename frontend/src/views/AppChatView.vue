<template>
  <ChatInterface
    :config="chatConfig"
    :stream-function="streamAppChat"
    :send-function="sendAppPrompt"
  />
</template>

<script setup>
import ChatInterface from '../components/chat/ChatInterface.vue';
import { sendAppPrompt, streamAppChat } from '../services/chatService';

const chatConfig = {
  title: 'AI 应用聊天',
  description: '每次进入都会生成独立会话，方便与后端联调时区分不同的对话记录。',
  placeholder: '请输入你的问题，回车发送（Shift + Enter 换行）',
  loadingText: '回应生成中...',
  theme: 'blue'
};
</script>