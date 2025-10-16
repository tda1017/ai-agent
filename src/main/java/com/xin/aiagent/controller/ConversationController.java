package com.xin.aiagent.controller;

import com.xin.aiagent.entity.Conversation;
import com.xin.aiagent.entity.Message;
import com.xin.aiagent.security.UserPrincipal;
import com.xin.aiagent.service.ConversationService;
import com.xin.aiagent.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话管理控制器
 * 提供对话的创建、查询、删除等 REST API 接口
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;

    /**
     * 创建新对话
     * 为当前用户创建一个新的对话，支持自定义标题
     *
     * @param body 请求体，可选，包含 title 字段用于设置对话标题
     * @return 包含新创建的对话 ID 和标题的 Map
     */
    @PostMapping
    public java.util.Map<String, Object> create(@RequestBody(required = false) java.util.Map<String, String> body) {
        Long uid = currentUserId();
        if (log.isDebugEnabled()) {
            log.debug("Create conversation request: userId={}, body={}", uid, body);
        }
        String title = body != null ? body.get("title") : null;
        Long id = conversationService.ensureConversation(uid, null, title);
        // Map.of 不允许 null 值，这里用可空的 HashMap 以避免 NPE
        java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
        resp.put("id", id);
        resp.put("title", title); // 允许为 null，前端会兜底显示“新对话”
        return resp;
    }

    /**
     * 查询当前用户的对话列表
     * 按更新时间降序返回，支持限制返回数量
     *
     * @param limit 返回的最大对话数量，默认 20 条
     * @return 对话列表
     */
    @GetMapping
    public List<Conversation> list(@RequestParam(defaultValue = "20") int limit) {
        Long uid = currentUserId();
        if (log.isDebugEnabled()) {
            log.debug("List conversations request: userId={}, limit={}", uid, limit);
        }
        return conversationService.listByUser(uid, limit);
    }

    /**
     * 查询指定对话的消息历史
     * 支持分页加载，通过 lastId 参数实现增量获取
     *
     * @param id 对话 ID
     * @param lastId 上次查询的最后一条消息 ID，用于分页，默认 0 表示从头开始
     * @param limit 返回的最大消息数量，默认 50 条
     * @return 消息列表
     */
    @GetMapping("/{id}/messages")
    public List<Message> messages(@PathVariable("id") Long id,
                                  @RequestParam(defaultValue = "0") Long lastId,
                                  @RequestParam(defaultValue = "50") int limit) {
        // 可加归属校验：确认 id 属于 currentUserId
        if (log.isDebugEnabled()) {
            log.debug("List messages request: conversationId={}, lastId={}, limit={}", id, lastId, limit);
        }
        return messageService.list(id, lastId, limit);
    }

    /**
     * 软删除指定对话
     * 将对话标记为已删除，不从数据库中物理删除
     *
     * @param id 对话 ID
     * @return 包含成功标识的 Map
     */
    @DeleteMapping("/{id}")
    public Object delete(@PathVariable("id") Long id) {
        Long uid = currentUserId();
        if (log.isInfoEnabled()) {
            log.info("Delete conversation request: userId={}, conversationId={}", uid, id);
        }
        conversationService.softDelete(uid, id);
        java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        return resp;
    }

    /**
     * 兼容端点：部分代理/网关可能拦截 DELETE 方法
     * 提供 POST /{id}/delete 以保证前端在 403/405 时可回退
     */
    @PostMapping("/{id}/delete")
    public Object deleteCompat(@PathVariable("id") Long id) {
        Long uid = currentUserId();
        if (log.isInfoEnabled()) {
            log.info("Delete conversation (compat POST) request: userId={}, conversationId={}", uid, id);
        }
        conversationService.softDelete(uid, id);
        java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        return resp;
    }

    /**
     * 获取当前登录用户的 ID
     * 从 Spring Security 上下文中提取用户信息
     *
     * @return 当前用户 ID
     * @throws RuntimeException 如果未找到用户信息
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (log.isTraceEnabled()) {
                log.trace("Resolved current user: userId={}, username={}", principal.getUserId(), principal.getUsername());
            }
            return principal.getUserId();
        }
        if (log.isWarnEnabled()) {
            log.warn("Failed to resolve current user from SecurityContext. auth={}", auth);
        }
        throw new RuntimeException("未找到当前用户信息");
    }
}
