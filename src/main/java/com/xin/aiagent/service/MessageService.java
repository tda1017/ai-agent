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
        m.setConversationId(conversationId);
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
