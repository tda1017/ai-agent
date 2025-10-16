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

    @PostMapping
    public java.util.Map<String, Object> create(@RequestBody(required = false) java.util.Map<String, String> body) {
        Long uid = currentUserId();
        String title = body != null ? body.get("title") : null;
        Long id = conversationService.ensureConversation(uid, null, title);
        // Map.of 不允许 null 值，这里用可空的 HashMap 以避免 NPE
        java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
        resp.put("id", id);
        resp.put("title", title); // 允许为 null，前端会兜底显示“新对话”
        return resp;
    }

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
