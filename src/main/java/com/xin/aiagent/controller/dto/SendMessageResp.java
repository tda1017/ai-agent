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
