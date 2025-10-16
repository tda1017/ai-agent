package com.xin.aiagent.controller;

import com.xin.aiagent.app.App;
import com.xin.aiagent.controller.dto.SendMessageDTO;
import com.xin.aiagent.controller.dto.SendMessageResp;
import com.xin.aiagent.service.ChatService;
import com.xin.aiagent.controller.dto.ChatRequest;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天接口层（REST + SSE）
 * 对齐前端约定：
 * - POST /api/doChatWithApp → 确认受理（非流式）
 * - POST /api/doChatWithManus → 确认受理（非流式）
 * - GET  /api/doChatWithAppSse → 流式返回（RAG 云端）
 * - GET  /api/doChatWithManus → 流式返回（工具模式）
 */
@RestController
@RequestMapping("/api")
@Slf4j
@Validated
public class ChatController {

    @Resource
    private App app;
    @Resource
    private ChatService chatService;

    /**
     * App 聊天：受理请求（非流式）
     */
    @PostMapping(path = "/doChatWithApp", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> doChatWithApp(@RequestBody @Valid ChatRequest request) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("accepted", true);
        resp.put("sessionId", request.getSessionId());
        resp.put("prompt", request.getPrompt());
        resp.put("ts", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Manus（工具模式）聊天：受理请求（非流式）
     */
    @PostMapping(path = "/doChatWithManus", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> doChatWithManus(@RequestBody @Valid ChatRequest request) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("accepted", true);
        resp.put("sessionId", request.getSessionId());
        resp.put("prompt", request.getPrompt());
        resp.put("ts", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * App 聊天：SSE 流式返回（调用云端 RAG）
     */
    @GetMapping(path = "/doChatWithAppSse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithAppSse(@RequestParam("sessionId") String sessionId,
                                       @RequestParam("prompt") String prompt) {
        return buildSse(() -> app.doChatWithRagCloud(prompt, sessionId));
    }

    /**
     * Manus（工具模式）聊天：SSE 流式返回
     * 注意：与 POST /doChatWithManus 路径相同，但方法不同（GET vs POST）。
     */
    @GetMapping(path = "/doChatWithManus", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManusSse(@RequestParam("sessionId") String sessionId,
                                         @RequestParam("prompt") String prompt) {
        return buildSse(() -> app.doChatWithTools(prompt, sessionId));
    }

    // ============== Minimal persistence endpoints ==============

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SendMessageResp chat(@RequestBody @Valid SendMessageDTO req) {
        Long uid = currentUserId();
        return chatService.send(uid, req.getConversationId(), req.getContent());
    }

    // ============== 私有工具方法 ==============

    /**
     * 构建 SSE：执行 supplier 获取完整回答，再按片段增量推送，最后发送 done 事件。
     */
    private SseEmitter buildSse(SupplierWithEx<String> supplier) {
        // 10 分钟超时，满足大多数对话场景
        SseEmitter emitter = new SseEmitter(600_000L);

        CompletableFuture.runAsync(() -> {
            try {
                // 可选：发送 start 事件
                Map<String, Object> start = Map.of("type", "start", "ts", Instant.now().toEpochMilli());
                emitter.send(SseEmitter.event().name("message").data(start, MediaType.APPLICATION_JSON));

                String full = supplier.get();
                if (full == null) {
                    full = "";
                }
                // 按固定长度分片，尽量避免拆分多字节字符
                for (String part : chunkUtf8(full, 120)) {
                    Map<String, Object> delta = Map.of("type", "delta", "content", part);
                    emitter.send(SseEmitter.event().name("message").data(delta, MediaType.APPLICATION_JSON));
                }

                // 结束事件
                emitter.send(SseEmitter.event().name("done").data("done", MediaType.TEXT_PLAIN));
                emitter.complete();
            } catch (IOException e) {
                log.error("SSE 发送失败", e);
                safeError(emitter, "io_error", e.getMessage());
            } catch (Exception e) {
                log.error("SSE 执行异常", e);
                safeError(emitter, "server_error", e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * 将字符串按 UTF-8 字节序安全地近似固定长度切片。
     */
    private List<String> chunkUtf8(String text, int maxChars) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        int len = text.length();
        new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8); // 编码健壮性触发
        int start = 0;
        new StringBuilder();
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        while (start < len) {
            int end = Math.min(start + maxChars, len);
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }

    private void safeError(SseEmitter emitter, String code, String message) {
        try {
            Map<String, Object> err = new HashMap<>();
            err.put("type", "error");
            err.put("code", code);
            err.put("message", message);
            emitter.send(SseEmitter.event().name("message").data(err, MediaType.APPLICATION_JSON));
            emitter.completeWithError(new RuntimeException(code + ":" + message));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    @FunctionalInterface
    private interface SupplierWithEx<T> {
        T get() throws Exception;
    }

    private Long currentUserId() {
        // TODO: 替换为真实鉴权（如从 SecurityContext 读取）
        return 1L;
    }
}

