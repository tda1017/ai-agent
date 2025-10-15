# 前端聊天历史追溯功能设计文档

> **设计原则**：数据结构优先，消除特殊情况，零破坏性，实用主义

## 1. 需求背景

### 1.1 当前问题
- 聊天消息仅存在前端内存中，刷新页面即丢失
- sessionId随机生成但不持久化，无法追溯历史会话
- 用户无法查看过往对话记录
- 多设备无法同步聊天历史

### 1.2 目标
实现聊天历史的持久化存储和追溯查看，允许用户：
- 查看所有历史会话列表
- 切换到任意历史会话查看完整对话
- 创建新会话开始新对话
- 删除不需要的历史会话

## 2. 核心数据结构

### 2.1 Session（会话）
```typescript
interface Session {
  id: string;              // UUID，由前端生成
  title: string;           // 会话标题：用户首条问题的前30字
  createdAt: number;       // 创建时间戳
  updatedAt: number;       // 最后更新时间戳
}
```

### 2.2 Message（消息）
```typescript
interface Message {
  id: string;              // 消息ID：`${timestamp}_${role}`
  sessionId: string;       // 所属会话ID（外键）
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;       // 消息时间戳
}
```

### 2.3 数据关系
- 一个Session包含多条Message（1:N关系）
- Message通过sessionId关联到Session
- 删除Session时级联删除其所有Message

## 3. 架构设计

### 3.1 数据流向
```
用户输入 → useChat.handleSubmit()
    ↓
前端发送消息（POST /api/doChatWithManus）
    ↓
后端保存Message到数据库
    ↓
SSE流式返回响应（GET /api/doChatWithManus）
    ↓
前端实时渲染 + 后端保存完整响应
    ↓
前端重新加载会话列表（GET /api/sessions）
```

**数据唯一真实来源（Single Source of Truth）**：后端数据库

### 3.2 组件结构
```
AppChatView / ManusChatView
├── SessionSidebar (新增)
│   ├── 会话列表
│   ├── 新建会话按钮
│   └── 删除会话按钮
└── ChatInterface (保持不变)
    ├── 消息列表
    └── 输入框
```

## 4. 前端实现方案

### 4.1 新增组件：SessionSidebar.vue
**职责**：展示会话列表、处理会话切换、创建、删除操作

**主要功能**：
```vue
<template>
  <aside class="session-sidebar">
    <div class="sidebar-header">
      <h2>历史对话</h2>
      <button @click="createNewSession">+ 新对话</button>
    </div>
    
    <div class="session-list">
      <div
        v-for="session in sessions"
        :key="session.id"
        :class="['session-item', { active: session.id === currentSessionId }]"
        @click="switchSession(session.id)"
      >
        <div class="session-title">{{ session.title }}</div>
        <div class="session-meta">
          {{ formatTime(session.updatedAt) }}
        </div>
        <button 
          class="delete-btn"
          @click.stop="deleteSession(session.id)"
        >
          删除
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchSessions, deleteSessionById } from '@/services/chatService';

const emit = defineEmits(['session-switch', 'session-create']);
const props = defineProps({
  currentSessionId: String
});

const sessions = ref([]);

async function loadSessions() {
  sessions.value = await fetchSessions();
}

function createNewSession() {
  emit('session-create');
}

function switchSession(sessionId) {
  emit('session-switch', sessionId);
}

async function deleteSession(sessionId) {
  if (!confirm('确定删除此对话？')) return;
  await deleteSessionById(sessionId);
  await loadSessions();
}

onMounted(() => {
  loadSessions();
});
</script>
```

### 4.2 扩展 useChat.js
**保持现有逻辑不变，新增历史加载功能**：

```javascript
// 新增功能
async function loadHistory(sessionId) {
  try {
    const history = await fetchSessionMessages(sessionId);
    messages.value = history;
    nextTick(() => {
      startScrollIntoView();
    });
  } catch (error) {
    console.error('加载历史失败', error);
  }
}

function createNewSession() {
  sessionId.value = generateSessionId();
  messages.value = [];
  draft.value = '';
}

function switchSession(newSessionId) {
  sessionId.value = newSessionId;
  messages.value = [];
  draft.value = '';
  loadHistory(newSessionId);
}

// 现有的handleSubmit()逻辑保持不变
```

### 4.3 扩展 chatService.js
**新增API调用函数**：

```javascript
// 获取所有会话列表
export async function fetchSessions() {
  const { data } = await httpClient.get('/sessions');
  return data;
}

// 获取指定会话的所有消息
export async function fetchSessionMessages(sessionId) {
  const { data } = await httpClient.get(`/sessions/${sessionId}/messages`);
  return data;
}

// 删除指定会话
export async function deleteSessionById(sessionId) {
  await httpClient.delete(`/sessions/${sessionId}`);
}
```

### 4.4 修改视图组件
**AppChatView.vue / ManusChatView.vue 集成 SessionSidebar**：

```vue
<template>
  <div class="chat-view-container">
    <SessionSidebar
      :current-session-id="sessionId"
      @session-switch="handleSessionSwitch"
      @session-create="handleSessionCreate"
    />
    <ChatInterface
      :config="chatConfig"
      :stream-function="streamManusChat"
      :send-function="sendManusPrompt"
    />
  </div>
</template>

<script setup>
import SessionSidebar from '@/components/chat/SessionSidebar.vue';
import { useChat } from '@/composables/useChat';

const { sessionId, switchSession, createNewSession } = useChat(
  streamManusChat,
  sendManusPrompt
);

function handleSessionSwitch(newSessionId) {
  switchSession(newSessionId);
}

function handleSessionCreate() {
  createNewSession();
}
</script>

<style scoped>
.chat-view-container {
  display: flex;
  height: 100%;
}
</style>
```

## 5. 后端实现方案

### 5.1 数据库设计

**Session表**：
```sql
CREATE TABLE sessions (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  INDEX idx_updated_at (updated_at DESC)
);
```

**Message表**：
```sql
CREATE TABLE messages (
  id VARCHAR(50) PRIMARY KEY,
  session_id VARCHAR(36) NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  timestamp BIGINT NOT NULL,
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
  INDEX idx_session_timestamp (session_id, timestamp)
);
```

### 5.2 新增API接口

#### 5.2.1 获取会话列表
```
GET /api/sessions

Response:
[
  {
    "id": "uuid",
    "title": "如何实现聊天历史功能？",
    "createdAt": 1728912345000,
    "updatedAt": 1728912567000
  }
]
```

#### 5.2.2 获取会话消息
```
GET /api/sessions/{sessionId}/messages

Response:
[
  {
    "id": "1728912345_user",
    "sessionId": "uuid",
    "role": "user",
    "content": "如何实现聊天历史功能？",
    "timestamp": 1728912345000
  },
  {
    "id": "1728912346_assistant",
    "sessionId": "uuid",
    "role": "assistant",
    "content": "可以通过...",
    "timestamp": 1728912346000
  }
]
```

#### 5.2.3 删除会话
```
DELETE /api/sessions/{sessionId}

Response:
{
  "success": true
}
```

### 5.3 修改现有接口（内部逻辑）

**POST /api/doChatWithManus**（保存用户消息）：
```java
@PostMapping("/doChatWithManus")
public ResponseEntity<Map<String, Object>> doChatWithManus(@RequestBody ChatRequest request) {
    // 原有逻辑保持不变
    Map<String, Object> resp = new HashMap<>();
    resp.put("accepted", true);
    resp.put("sessionId", request.getSessionId());
    resp.put("prompt", request.getPrompt());
    resp.put("ts", Instant.now().toString());
    
    // 新增：保存用户消息到数据库
    messageService.saveUserMessage(
        request.getSessionId(),
        request.getPrompt()
    );
    
    return ResponseEntity.ok(resp);
}
```

**GET /api/doChatWithManus**（SSE流式，保存AI消息）：
```java
@GetMapping("/doChatWithManus")
public SseEmitter doChatWithManusSse(@RequestParam String sessionId, @RequestParam String prompt) {
    return buildSse(() -> {
        String fullResponse = app.doChatWithTools(prompt, sessionId);
        
        // 新增：保存AI完整响应到数据库
        messageService.saveAssistantMessage(sessionId, fullResponse);
        
        return fullResponse;
    });
}
```

### 5.4 Service层实现

**SessionService.java**：
```java
@Service
public class SessionService {
    @Autowired
    private SessionRepository sessionRepository;
    
    // 获取所有会话（按更新时间倒序）
    public List<Session> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }
    
    // 创建或更新会话（首次收到消息时创建）
    public void upsertSession(String sessionId, String firstUserMessage) {
        Session session = sessionRepository.findById(sessionId)
            .orElse(new Session(sessionId, extractTitle(firstUserMessage)));
        session.setUpdatedAt(System.currentTimeMillis());
        sessionRepository.save(session);
    }
    
    // 提取会话标题（用户首条问题的前30字）
    private String extractTitle(String message) {
        return message.length() > 30 
            ? message.substring(0, 30) + "..." 
            : message;
    }
    
    // 删除会话（级联删除消息）
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
```

**MessageService.java**：
```java
@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SessionService sessionService;
    
    // 保存用户消息
    public void saveUserMessage(String sessionId, String content) {
        Message message = new Message(
            System.currentTimeMillis() + "_user",
            sessionId,
            "user",
            content,
            System.currentTimeMillis()
        );
        messageRepository.save(message);
        
        // 更新会话时间（如果是新会话则创建）
        sessionService.upsertSession(sessionId, content);
    }
    
    // 保存AI消息
    public void saveAssistantMessage(String sessionId, String content) {
        Message message = new Message(
            System.currentTimeMillis() + "_assistant",
            sessionId,
            "assistant",
            content,
            System.currentTimeMillis()
        );
        messageRepository.save(message);
        
        // 更新会话时间
        sessionService.upsertSession(sessionId, null);
    }
    
    // 获取会话的所有消息（按时间升序）
    public List<Message> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}
```

## 6. 实现步骤（推荐顺序）

### 阶段1：后端基础设施
1. 创建数据库表（Session + Message）
2. 创建Entity、Repository
3. 实现SessionService、MessageService
4. 修改现有接口的内部逻辑（保存消息）
5. 测试消息保存功能

### 阶段2：后端新API
1. 实现GET /api/sessions接口
2. 实现GET /api/sessions/{id}/messages接口
3. 实现DELETE /api/sessions/{id}接口
4. API测试

### 阶段3：前端基础功能
1. 扩展chatService.js（新增API调用）
2. 扩展useChat.js（新增loadHistory、switchSession、createNewSession）
3. 测试数据加载和会话切换

### 阶段4：前端UI
1. 创建SessionSidebar.vue组件
2. 集成到AppChatView和ManusChatView
3. 调整样式和布局
4. 端到端测试

## 7. 边界情况处理

### 7.1 空会话列表
- 首次使用时，自动创建一个新会话
- 显示欢迎提示："开始你的第一次对话"

### 7.2 会话切换时的加载状态
- 显示Loading指示器
- 禁用输入框，直到消息加载完成

### 7.3 删除当前会话
- 删除后自动切换到最近的其他会话
- 如果没有其他会话，创建新会话

### 7.4 并发问题
- 用户快速切换会话时，取消之前的SSE连接
- 使用sessionId作为请求标识，避免消息串话

### 7.5 会话标题为空
- 如果用户首条消息为空（不应该发生），使用"未命名对话"作为标题

## 8. 非功能性需求

### 8.1 性能
- 会话列表分页：单次最多返回50条会话
- 消息列表不分页（假设单会话消息量<1000条）
- 前端虚拟滚动（如果单会话消息量>100条）

### 8.2 用户体验
- 会话切换<300ms（本地网络）
- 历史消息加载<500ms
- 删除会话有确认提示

### 8.3 数据一致性
- 后端为数据唯一真实来源
- 前端消息列表仅作为临时缓存
- 切换会话时重新从后端加载，避免脏数据

## 9. 未来扩展（不在当前范围）

以下功能**暂不实现**，避免过度设计：
- ❌ 会话搜索/筛选
- ❌ 会话标签/分类
- ❌ 手动编辑会话标题
- ❌ 导出会话为Markdown/PDF
- ❌ 会话分享功能
- ❌ 消息编辑/重新生成
- ❌ 多用户权限控制（当前假设单用户）

如有真实需求，再单独评估。

## 10. 风险评估

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 破坏现有流式聊天功能 | 高 | 保持ChatController和useChat的现有逻辑不变，仅新增保存逻辑 |
| 数据库写入影响SSE性能 | 中 | 异步保存消息，不阻塞SSE响应 |
| 前端状态管理混乱 | 中 | 明确后端为唯一数据源，前端只做临时缓存 |
| 用户快速切换导致消息串话 | 中 | 在useChat中检查sessionId一致性 |

## 11. 总结

**核心原则**：
1. **数据结构优先**：Session(1) → Messages(N)，后端为唯一数据源
2. **消除特殊情况**：不区分临时/正式会话，统一自动保存
3. **零破坏性**：保持ChatInterface和useChat核心逻辑不变
4. **实用主义**：只做真正需要的功能，拒绝过度设计

**实现路径**：
- 后端新增3个接口 + 修改现有接口内部逻辑
- 前端新增1个组件 + 扩展1个composable
- 保持ChatInterface组件零改动

**预期成果**：
- 用户可以查看、切换、删除历史会话
- 刷新页面不丢失历史记录
- 多设备同步聊天历史
- 不破坏现有流式聊天体验
