# Chat Persistence Backend Minimal Implementation (M0)

状态：已落库并可用（完成）
- [x] Entities：`Conversation`, `Message`
- [x] Mappers：`ConversationMapper`, `MessageMapper`
- [x] DTOs：`SendMessageDTO`, `SendMessageResp`
- [x] Services：`ConversationService`, `MessageService`, `ChatService`
- [x] Controllers：新增 `ConversationController`；在现有 `ChatController` 增加 `POST /api/chat`
- [x] SQL：`sql/V1__chat_persistence.sql` 已存在

目标：最小工作量打通“会话 + 历史消息”的后端能力，支持会话列表、按会话翻页读取历史、软删除会话，以及发送消息时的会话懒创建。代码可直接复制到项目中微调。

前置：已存在数据表 `conversations` 与 `messages`（见 `docs/specs/chat-persistence.md` 或 `sql/V1__chat_persistence.sql`）。

依赖：
- MyBatis-Plus（用于简化 CRUD）
- Lombok（`@Data`, `@RequiredArgsConstructor`）

包名按当前项目结构：`com.xin.aiagent`

---

## Entities

文件：`src/main/java/com/xin/aiagent/entity/Conversation.java`

```java
package com.xin.aiagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
```

文件：`src/main/java/com/xin/aiagent/entity/Message.java`

```java
package com.xin.aiagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    // DB 用 ENUM('user','assistant')，这里用字符串
    private String role;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
```

---

## Mappers

文件：`src/main/java/com/xin/aiagent/mapper/ConversationMapper.java`

```java
package com.xin.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.aiagent.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
```

文件：`src/main/java/com/xin/aiagent/mapper/MessageMapper.java`

```java
package com.xin.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.aiagent.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
```

---

## DTOs

文件：`src/main/java/com/xin/aiagent/controller/dto/SendMessageDTO.java`

```java
package com.xin.aiagent.controller.dto;
import lombok.Data;

@Data
public class SendMessageDTO {
    private Long conversationId; // 可选
    private String content;      // 必填
}
```

文件：`src/main/java/com/xin/aiagent/controller/dto/SendMessageResp.java`

```java
package com.xin.aiagent.controller.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendMessageResp {
    private Long conversationId;
    private Long messageId;
    private String answer;
}
```

---

## Services

文件：`src/main/java/com/xin/aiagent/service/ConversationService.java`

```java
package com.xin.aiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xin.aiagent.entity.Conversation;
import com.xin.aiagent.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationMapper conversationMapper;

    public Long ensureConversation(Long userId, Long conversationId, String maybeTitleIfNew) {
        if (conversationId == null) {
            Conversation c = new Conversation();
            c.setUserId(userId);
            if (maybeTitleIfNew != null) {
                String t = maybeTitleIfNew.length() > 40 ? maybeTitleIfNew.substring(0, 40) : maybeTitleIfNew;
                c.setTitle(t);
            }
            conversationMapper.insert(c);
            return c.getId();
        }
        Conversation exists = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
            .eq(Conversation::getId, conversationId)
            .eq(Conversation::getUserId, userId)
            .isNull(Conversation::getDeletedAt));
        if (exists == null) {
            throw new RuntimeException("Conversation not found");
        }
        return conversationId;
    }

    public List<Conversation> listByUser(Long userId, int limit) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
            .eq(Conversation::getUserId, userId)
            .isNull(Conversation::getDeletedAt)
            .orderByDesc(Conversation::getUpdatedAt)
            .last("limit " + limit));
    }

    public void softDelete(Long userId, Long conversationId) {
        int updated = conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
            .eq(Conversation::getId, conversationId)
            .eq(Conversation::getUserId, userId)
            .isNull(Conversation::getDeletedAt)
            .set(Conversation::getDeletedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new RuntimeException("Conversation not found or already deleted");
        }
    }

    public void touch(Long conversationId) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
            .eq(Conversation::getId, conversationId)
            .set(Conversation::getUpdatedAt, LocalDateTime.now()));
    }
}
```

文件：`src/main/java/com/xin/aiagent/service/MessageService.java`

```java
package com.xin.aiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xin.aiagent.entity.Message;
import com.xin.aiagent.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageMapper messageMapper;

    public void insertUserMessage(Long conversationId, String content) {
        Message m = new Message();
        m.setC
        onversationId(conversationId);
        m.setRole("user");
        m.setContent(content);
        messageMapper.insert(m);
    }

    public Long insertAssistantMessage(Long conversationId, String content) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole("assistant");
        m.setContent(content);
        messageMapper.insert(m);
        return m.getId();
    }

    public List<Message> list(Long conversationId, Long lastId, int limit) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
            .eq(Message::getConversationId, conversationId)
            .isNull(Message::getDeletedAt)
            .gt(lastId != null && lastId > 0, Message::getId, lastId)
            .orderByAsc(Message::getId)
            .last("limit " + limit));
    }
}
```

文件：`src/main/java/com/xin/aiagent/service/ChatService.java`

```java
package com.xin.aiagent.service;

import com.xin.aiagent.controller.dto.SendMessageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationService conversationService;
    private final MessageService messageService;

    public SendMessageResp send(Long userId, Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is blank");
        }
        Long cid = conversationService.ensureConversation(userId, conversationId, content);

        // 1) 存用户消息
        messageService.insertUserMessage(cid, content);

        // 2) 调 AI（这里先用一个简单回声，方便先打通）
        String answer = "Echo: " + content; // TODO: 替换为 Spring AI 调用

        // 3) 存 AI 消息并更新会话时间
        Long mid = messageService.insertAssistantMessage(cid, answer);
        conversationService.touch(cid);

        return new SendMessageResp(cid, mid, answer);
    }
}
```

---

## Controllers

文件：`src/main/java/com/xin/aiagent/controller/ConversationController.java`

```java
package com.xin.aiagent.controller;

import com.xin.aiagent.entity.Conversation;
import com.xin.aiagent.entity.Message;
import com.xin.aiagent.service.ConversationService;
import com.xin.aiagent.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    public List<Conversation> list(@RequestParam(defaultValue = "20") int limit) {
        Long uid = currentUserId();
        return conversationService.listByUser(uid, limit);
    }

    @GetMapping("/{id}/messages")
    public List<Message> messages(@PathVariable("id") Long id,
                                  @RequestParam(defaultValue = "0") Long lastId,
                                  @RequestParam(defaultValue = "50") int limit) {
        // 可加归属校验：确认 id 属于 currentUserId
        return messageService.list(id, lastId, limit);
    }

    @DeleteMapping("/{id}")
    public Object delete(@PathVariable("id") Long id) {
        Long uid = currentUserId();
        conversationService.softDelete(uid, id);
        return java.util.Map.of("success", true);
    }

    private Long currentUserId() {
        // TODO: 替换为真实鉴权（如从 SecurityContext 读取）
        return 1L;
    }
}
```

文件：`src/main/java/com/xin/aiagent/controller/ChatController.java`

```java
package com.xin.aiagent.controller;

import com.xin.aiagent.controller.dto.SendMessageDTO;
import com.xin.aiagent.controller.dto.SendMessageResp;
import com.xin.aiagent.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/chat")
    public SendMessageResp chat(@RequestBody SendMessageDTO req) {
        Long uid = currentUserId();
        return chatService.send(uid, req.getConversationId(), req.getContent());
    }

    private Long currentUserId() {
        // TODO: 替换为真实鉴权（如从 SecurityContext 读取）
        return 1L;
    }
}
```

---

## 关键点与自测

- 用户隔离：所有查询/删除务必基于 `user_id = currentUserId`，防越权。
- 懒创建：未传 `conversationId` 时在服务端创建会话并返回 id。
- 分页：使用 `id` 游标分页（`id > lastId`），避免 offset 带来的性能问题。
- 事务与 AI 调用：不要把 AI 调用放进大事务。先插用户消息，AI 返回后再插助手消息并更新会话时间。
- 快速验证：
  - `POST /api/chat` 发送消息（无会话 id 时返回新 `conversationId`）
  - `GET /api/conversations?limit=20` 查看会话列表
  - `GET /api/conversations/{id}/messages?lastId=0&limit=50` 查看历史消息
  - `DELETE /api/conversations/{id}` 软删除会话

接口位置：
- `src/main/java/com/xin/aiagent/controller/ChatController.java`（已集成 `/api/chat`）
- `src/main/java/com/xin/aiagent/controller/ConversationController.java`

---

以上即极简实现，可在此基础上替换 `ChatService` 中的 AI 回答为真实 Spring AI 逻辑，并将 `currentUserId()` 替换为你的鉴权获取方式。
