package com.xin.aiagent.service;

import com.xin.aiagent.controller.dto.SendMessageResp;
import com.xin.aiagent.app.App;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final App app;

    public SendMessageResp send(Long userId, Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is blank");
        }
        Long cid = conversationService.ensureConversation(userId, conversationId, content);

        // 1) 存用户消息
        messageService.insertUserMessage(cid, content);

        // 2) 调用 Spring AI（与 SSE 端一致的能力），获得完整回答用于回退
        String answer = app.doChatWithTools(content, String.valueOf(cid));

        // 3) 存 AI 消息并更新会话时间
        Long mid = messageService.insertAssistantMessage(cid, answer);
        conversationService.touch(cid);

        return new SendMessageResp(cid, mid, answer);
    }
}
