package com.xin.aiagent.controller;

import com.xin.aiagent.entity.User;
import com.xin.aiagent.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.xin.aiagent.common.Result;
import com.xin.aiagent.common.ResultCode;

/**
 * 认证接口（M0）：提供登录与自检占位。
 *
 * - POST /api/auth/login：验证用户名密码，签发 JWT（载荷包含 uid/roles/permissions）。
 * - GET  /api/auth/me：占位接口，后续可回显当前用户信息。
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    /**
     * 登录：比对凭证，成功后签发 JWT。
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.loginAndIssueToken(req.getUsername(), req.getPassword());
        User user = authService.loadEnabledUserByUsername(req.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
        return ResponseEntity.ok(Result.of(ResultCode.OK, "登录成功", data));
    }

    /**
     * 注册：创建新用户并返回基本信息。
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.registerNewUser(req.getUsername(), req.getPassword(), req.getEmail());
        Map<String, Object> data = Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        );
        return ResponseEntity.ok(Result.of(ResultCode.OK, "注册成功", data));
    }



    /**
     * 获取当前用户信息（占位）：后续可解析请求头 JWT 并回显用户详情。
     */
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(Result.ok(Map.of("message", "OK")));
    }

    /** 登录请求体 */
    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    /** 注册请求体 */
    @Data
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @Email(message = "invalid_email", regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private String email;
    }
}
