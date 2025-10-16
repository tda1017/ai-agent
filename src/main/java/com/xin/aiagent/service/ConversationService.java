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
