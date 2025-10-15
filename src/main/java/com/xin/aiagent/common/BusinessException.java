package com.xin.aiagent.common;

/**
 * 业务异常，承载统一业务码与消息。
 */
public class BusinessException extends RuntimeException {
    private final ResultCode code;

    public BusinessException(ResultCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResultCode getCodeEnum() { return code; }
}
