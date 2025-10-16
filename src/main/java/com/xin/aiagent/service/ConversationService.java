package com.xin.aiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xin.aiagent.entity.Conversation;
import com.xin.aiagent.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话服务层
 * 负责对话的业务逻辑处理，包括创建、查询、删除、更新等操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
    private final ConversationMapper conversationMapper;

    /**
     * 确保对话存在
     * 如果传入的 conversationId 为 null，则创建新对话；否则验证对话是否存在且属于该用户
     *
     * @param userId 用户 ID
     * @param conversationId 对话 ID，可为 null
     * @param maybeTitleIfNew 如果需要创建新对话，设置的标题（可选）
     * @return 对话 ID
     * @throws RuntimeException 如果指定的 conversationId 不存在或不属于该用户
     */
    public Long ensureConversation(Long userId, Long conversationId, String maybeTitleIfNew) {
        if (conversationId == null) {
            Conversation c = new Conversation();
            c.setUserId(userId);
            if (maybeTitleIfNew != null && !maybeTitleIfNew.isBlank()) {
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

    /**
     * 查询用户的对话列表
     * 按更新时间降序排列，只返回未删除的对话
     *
     * @param userId 用户 ID
     * @param limit 返回的最大数量
     * @return 对话列表
     */
    public List<Conversation> listByUser(Long userId, int limit) {
        if (log.isDebugEnabled()) {
            log.debug("List conversations: userId={}, limit={}", userId, limit);
        }
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .isNull(Conversation::getDeletedAt)
                .orderByDesc(Conversation::getUpdatedAt)
                .last("limit " + limit));
    }

    /**
     * 软删除对话
     * 将对话标记为已删除（设置 deletedAt 字段），而不是物理删除
     * 采用幂等设计：如果对话不存在或已被删除，也视为成功
     *
     * @param userId 用户 ID，用于权限验证
     * @param conversationId 对话 ID
     */
    public void softDelete(Long userId, Long conversationId) {
        if (log.isInfoEnabled()) {
            log.info("Soft delete conversation start: userId={}, conversationId={}", userId, conversationId);
        }
        int updated = conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, userId)
                .isNull(Conversation::getDeletedAt)
                .set(Conversation::getDeletedAt, LocalDateTime.now()));
        // 幂等删除：如果未更新任何行（不存在或已删除），也视为成功，避免打断前端流程
        // Never break userspace —— 删除操作应当是可重复的
        if (updated == 0) {
            if (log.isWarnEnabled()) {
                log.warn("Soft delete no-op: conversation not found or already deleted. userId={}, conversationId={}", userId, conversationId);
            }
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Soft delete success: affectedRows={}, userId={}, conversationId={}", updated, userId, conversationId);
        }
    }

    /**
     * 更新对话的更新时间
     * 当对话中有新消息时调用，保持对话列表的排序正确性
     *
     * @param conversationId 对话 ID
     */
    public void touch(Long conversationId) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getUpdatedAt, LocalDateTime.now()));
    }
}
