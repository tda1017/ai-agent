package com.xin.aiagent.controller.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    private Long conversationId; // optional
    private String content;      // required
}
