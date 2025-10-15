# 聊天历史持久化规范 (M0 极简版)

**目标**：为个人 AI Agent 添加基本的聊天历史记录和回溯功能。

**原则**：最小可用、不破坏现有接口、先跑起来再优化。

## 1. 范围

- **覆盖**：用户与 AI 的单聊对话历史
- **核心功能**：
  - 保存每次对话的问答记录
  - 按会话查看历史消息
  - 简单的分页翻看
- **不做**：多人群聊、消息撤回/编辑、已读状态、实时推送

## 2. 数据模型

**原则**：最小字段、用 id 排序、软删除。

### 2.1 会话表 `conversations`

```sql
CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '所属用户',
  title VARCHAR(200) COMMENT '会话标题（可选，如首条消息摘要）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL COMMENT '软删除',
  INDEX idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';
```

**说明**：
- 一个会话就是一次连续的对话，用 sessionId 关联
- `updated_at` 自动更新，用于会话列表排序
- `deleted_at` 不为空表示已删除，查询时过滤

### 2.2 消息表 `messages`

```sql
CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL COMMENT '所属会话',
  role ENUM('user', 'assistant') NOT NULL COMMENT '消息角色',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL COMMENT '软删除',
  INDEX idx_conversation_id (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
```

**说明**：
- `role`：user = 用户提问，assistant = AI 回答
- `id` 自增就是顺序，不需要额外的 seq
- `content` 就是纯文本，现在不支持图片/文件
- 分页用 `WHERE id > last_id ORDER BY id LIMIT N` 就够了

## 3. 分页协议

**最简单的办法**：用 `id` 分页。

```sql
-- 第一页
SELECT * FROM messages 
WHERE conversation_id = ? AND deleted_at IS NULL 
ORDER BY id 
LIMIT 50;

-- 下一页（last_id 是上一页最后一条的 id）
SELECT * FROM messages 
WHERE conversation_id = ? AND deleted_at IS NULL AND id > last_id 
ORDER BY id 
LIMIT 50;
```

**API 参数**：`?lastId=123&limit=50`

## 4. 写入路径

用户发送消息后：

1. **存用户问题**：`INSERT INTO messages (conversation_id, role, content) VALUES (?, 'user', ?)`
2. **调用 AI**：让 Spring AI 处理
3. **存 AI 回答**：`INSERT INTO messages (conversation_id, role, content) VALUES (?, 'assistant', ?)`
4. **更新会话**：`UPDATE conversations SET updated_at = NOW() WHERE id = ?`

就这么简单。不需要事务、不需要幂等、不需要 outbox。

## 5. 读取路径

**会话列表**：
```sql
SELECT * FROM conversations 
WHERE user_id = ? AND deleted_at IS NULL 
ORDER BY updated_at DESC 
LIMIT 20;
```

**历史消息**：
```sql
SELECT * FROM messages 
WHERE conversation_id = ? AND deleted_at IS NULL 
ORDER BY id 
LIMIT 50;
```

就这么多。

## 6. API 契约

对齐现有接口风格，增加 3 个接口：

### 6.0 会话创建（懒创建）

为保持极简与向后兼容：当发送消息请求未携带 `conversationId` 时，后端自动创建会话并在响应中返回 `conversationId`。若请求包含有效 `conversationId`，则复用该会话。

约束：所有与数据交互的请求都必须在服务端基于 `user_id` 做权限隔离校验（`WHERE user_id = currentUserId`）。

### 6.1 会话列表

**GET** `/api/conversations?limit=20`

Response:
```json
{
  "data": [
    {
      "id": 1,
      "title": "如何学习 Java",
      "updatedAt": "2025-01-15T10:00:00Z"
    }
  ]
}
```

### 6.2 历史消息

**GET** `/api/conversations/{id}/messages?lastId=0&limit=50`

Response:
```json
{
  "data": [
    {
      "id": 1,
      "role": "user",
      "content": "如何学习 Java",
      "createdAt": "2025-01-15T10:00:00Z"
    },
    {
      "id": 2,
      "role": "assistant",
      "content": "Java 是一门...",
      "createdAt": "2025-01-15T10:00:05Z"
    }
  ]
}
```

### 6.3 删除会话

**DELETE** `/api/conversations/{id}`

Response: `{"success": true}`

### 6.4 发送消息（向后兼容）

沿用现有发送接口路径（例如 `POST /api/chat` 或 `POST /api/messages`），只做「非破坏性扩展」：

Request（示例）：
```json
{
  "conversationId": 123,   // 可选，缺省表示新会话
  "content": "如何学习 Java"
}
```

Response（示例）：
```json
{
  "data": {
    "conversationId": 123,      
    "answer": "Java 是一门...", 
    "messageId": 2               
  }
}
```

说明：
- 未传 `conversationId` 时自动建会话并返回；已传则复用。
- 这是对响应体的向后兼容新增字段（添加字段不破坏既有客户端）。
- 服务端内部写入两条消息：一条 `role=user`，一条 `role=assistant`。

## 7. 实现步骤

1. **建表**：执行 DDL，创建 `conversations` 和 `messages` 表
2. **改 ChatController**：在现有逻辑里增加数据库写入；若请求无 `conversationId` 则懒创建后返回
3. **加 3 个接口**：会话列表、历史消息、删除会话（均基于 `user_id` 过滤）
4. **发送接口扩展**：响应中返回 `conversationId` 与 `messageId`（向后兼容新增字段）
5. **测试**：发几条消息，看看能不能查出来

（可选更简化）若不需要回收站语义，`DELETE /api/conversations/{id}` 可实现为硬删除；默认仍推荐软删除以避免误删风险。

就这么简单。
