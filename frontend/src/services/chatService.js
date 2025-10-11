import httpClient from './httpClient';
import { simulateStream } from '../utils/mockSse';

const SSE_ENDPOINTS = {
  app: '/api/doChatWithAppSse',
  manus: '/api/doChatWithManus'
};

const { VITE_USE_MOCK_STREAM, PROD } = import.meta.env;
const USE_MOCK_STREAM = !PROD && VITE_USE_MOCK_STREAM !== 'false';

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
  return openStream(
    SSE_ENDPOINTS.app,
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