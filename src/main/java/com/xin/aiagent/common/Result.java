package com.xin.aiagent.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应结构：{ code, message, data }
 */
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result() {}

    public Result(int code, String message, T data) {
        this.code = code; this.message = message; this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.OK.code(), "success", data);
    }

    public static <T> Result<T> of(ResultCode rc, String message) {
        return new Result<>(rc.code(), message != null ? message : rc.defaultMessage(), null);
    }

    public static <T> Result<T> of(ResultCode rc, String message, T data) {
        return new Result<>(rc.code(), message != null ? message : rc.defaultMessage(), data);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
