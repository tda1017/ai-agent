package com.xin.aiagent.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户认证主体，存储在 SecurityContext 中。
 * 包含 userId 和 username，避免在 Controller 中反复查询数据库。
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String username;
}
