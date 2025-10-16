import httpClient from './httpClient';
import { simulateStream } from '../utils/mockSse';

const SSE_ENDPOINTS = {
  app: '/api/doChatWithAppSse',
  manus: '/api/doChatWithManus'
};

const { VITE_USE_MOCK_STREAM, PROD } = import.meta.env;
// const USE_MOCK_STREAM = !PROD && VITE_USE_MOCK_STREAM !== 'false';
const USE_MOCK_STREAM = false;  // 强制关闭 mock，使用真实后端
const MOCK_RESPONSES = {
  app: [
    [
      '您好，我是 AI 应用助手。',
      '这里演示如何使用 SSE 实时渲染回答。',
      '待后端准备好接口后，只需要关闭 mock 即可切换为真实数据。'
    ],
    [
      '已经为您生成新的会话，可以开始体验。',
      '使用左下方输入框提交问题，会实时看到回复流式出现。'
    ]
  ],
  manus: [
    [
      '欢迎来到超级智能体面板。',
      '我能够协同多个能力，帮助你拆解复杂任务。',
      '当前展示的是演示数据，后端接入后将自动切换为真实结果。'
    ],
    [
      '智能体正在分析你的需求……',
      '它会逐步生成推理步骤与最终建议。'
    ]
  ]
};

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    query.append(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : '';
}

function safeParse(data) {
  if (typeof data !== 'string') {
    return data;
  }
  try {
    return JSON.parse(data);
  } catch (error) {
    return data;
  }
}

function openStream(endpoint, params, callbacks, mockKey) {
  const { onChunk, onComplete, onError } = callbacks ?? {};

  if (USE_MOCK_STREAM) {
    const preset = MOCK_RESPONSES[mockKey];
    const mockChunks = [
      { type: 'start', sessionId: params?.sessionId },
      ...preset[Math.floor(Math.random() * preset.length)].map((content) => ({
        type: 'delta',
        content
      })),
      { type: 'done' }
    ];
    return simulateStream(mockChunks, { onChunk, onComplete, onError, interval: 600 });
  }

  const url = `${endpoint}${buildQuery(params)}`;
  const source = new EventSource(url);

  source.onmessage = (event) => {
    const payload = safeParse(event.data);
    onChunk?.(payload);
  };
  source.onerror = (event) => {
    onError?.(event);
    source.close();
  };
  source.addEventListener('done', () => {
    onComplete?.();
    source.close();
  });

  return () => source.close();
}

export function streamAppChat(sessionId, prompt, callbacks) {
  // 临时使用 manus 模式（不使用 RAG），避免 embedding 404 错误
  return openStream(
    SSE_ENDPOINTS.manus,
    { sessionId, prompt },
    callbacks,
    'app'
  );
}

export function streamManusChat(sessionId, prompt, callbacks) {
  return openStream(
    SSE_ENDPOINTS.manus,
    { sessionId, prompt },
    callbacks,
    'manus'
  );
}

export async function sendAppPrompt(sessionId, prompt) {
  if (USE_MOCK_STREAM) {
    return Promise.resolve({ sessionId, prompt, status: 'mocked' });
  }
  const { data } = await httpClient.post('/doChatWithApp', { sessionId, prompt });
  return data;
}

export async function sendManusPrompt(sessionId, prompt) {
  if (USE_MOCK_STREAM) {
    return Promise.resolve({ sessionId, prompt, status: 'mocked' });
  }
  const { data } = await httpClient.post('/doChatWithManus', { sessionId, prompt });
  return data;
}

// 历史记录相关接口
export async function getConversations(limit = 20) {
  const response = await httpClient.get(`/conversations?limit=${limit}`);
  // 后端直接返回数组，不是 {data: []} 格式
  return Array.isArray(response.data) ? response.data : [];
}

export async function getMessages(conversationId, lastId = 0, limit = 50) {
  const response = await httpClient.get(
    `/conversations/${conversationId}/messages?lastId=${lastId}&limit=${limit}`
  );
  return Array.isArray(response.data) ? response.data : [];
}

export async function deleteConversation(conversationId) {
  try {
    const url = `/conversations/${conversationId}`
    const response = await httpClient.delete(url)
    // 记录一次成功日志，包含状态码与返回体结构
    // 注意：data 可能为 { success: true } 或空对象
    console.debug('[chatService] DELETE', {
      url,
      status: response?.status,
      data: response?.data
    })
    return response.data
  } catch (error) {
    // 失败时打印详细上下文，包含 code/status 与 message
    console.warn('[chatService] DELETE failed', {
      conversationId,
      code: error?.code || error?.response?.status,
      message: error?.message || error?.response?.data?.message,
      response: error?.response
    })
    // 如果是 403/405，可能是代理或网关不允许 DELETE，尝试兼容端点 POST /{id}/delete
    const status = error?.response?.status
    if (status === 403 || status === 405) {
      try {
        const compatUrl = `/conversations/${conversationId}/delete`
        const resp2 = await httpClient.post(compatUrl)
        console.debug('[chatService] Fallback POST delete ok', { compatUrl, status: resp2?.status, data: resp2?.data })
        return resp2.data
      } catch (e2) {
        console.warn('[chatService] Fallback POST delete failed', {
          conversationId,
          code: e2?.response?.status ?? e2?.code,
          message: e2?.response?.data?.message || e2?.message,
          response: e2?.response
        })
        throw e2
      }
    }
    throw error
  }
}

export async function createConversation(title) {
  const { data } = await httpClient.post('/conversations', title ? { title } : {});
  return data;
}

export async function sendMessage(conversationId, content) {
  const { data } = await httpClient.post('/chat', { 
    conversationId, 
    content 
  });
  return data;
}