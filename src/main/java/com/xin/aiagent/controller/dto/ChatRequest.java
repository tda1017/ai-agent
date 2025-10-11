package com.xin.aiagent.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求入参
 * 说明：前端会先以 POST 方式发送一次请求（用于排队/记录/权限校验等），
 * 随后通过 SSE 拉取流式结果。
 */
@Data
public class ChatRequest {

    /** 会话ID，用于串联对话记忆 */
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    /** 用户问题/提示词 */
    @NotBlank(message = "prompt 不能为空")
    private String prompt;
}

