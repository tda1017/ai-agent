package com.xin.aiagent.common;

/**
 * 统一业务码定义（保持简单直观）。
 */
public enum ResultCode {
    // 基础
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    CONFLICT(409, "Conflict"),
    INTERNAL_ERROR(500, "Internal Server Error"),

    // 校验与通用错误
    VALIDATION_ERROR(1000, "Validation failed"),

    // 认证相关
    INVALID_CREDENTIALS(1001, "Invalid username or password"),
    USER_NOT_FOUND_OR_DISABLED(1002, "User not found or disabled"),
    REGISTER_FAILED(1003, "Register failed"),
    USERNAME_EXISTS(1004, "Username already exists"),
    EMAIL_EXISTS(1005, "Email already exists");

    private final int code;
    private final String defaultMessage;

    ResultCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() { return code; }
    public String defaultMessage() { return defaultMessage; }
}
