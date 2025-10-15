package com.xin.aiagent.service;

import com.xin.aiagent.entity.User;

public interface AuthService {
    /**
     * 校验登录并返回签发的 JWT 字符串。
     */
    String loginAndIssueToken(String username, String rawPassword);

    /**
     * 根据用户名查询启用状态的用户。
     */
    User loadEnabledUserByUsername(String username);

    /**
     * 注册新用户（BCrypt 加密密码，默认启用）。
     * 若用户名或邮箱已存在，抛出运行时异常。
     */
    User registerNewUser(String username, String rawPassword, String email);
}
