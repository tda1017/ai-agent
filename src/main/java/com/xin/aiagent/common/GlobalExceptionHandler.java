package com.xin.aiagent.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        // 打印业务异常，便于排障
        log.warn("Business exception: code={}, message={}", ex.getCodeEnum(), ex.getMessage());
        ResultCode rc = ex.getCodeEnum();
        HttpStatus status = httpStatusOf(rc);
        return new ResponseEntity<>(Result.of(rc, ex.getMessage()), status);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<Void>> handleValidation(Exception ex) {
        log.warn("Validation exception: {}", ex.getMessage());
        String msg = extractValidationMessage(ex);
        return new ResponseEntity<>(Result.of(ResultCode.VALIDATION_ERROR, msg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return new ResponseEntity<>(Result.of(ResultCode.INVALID_CREDENTIALS, ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(Result.of(ResultCode.FORBIDDEN, ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOthers(Exception ex) {
        // 打印未知异常堆栈，避免 500 无日志的问题
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(Result.of(ResultCode.INTERNAL_ERROR, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static HttpStatus httpStatusOf(ResultCode rc) {
        return switch (rc) {
            case OK -> HttpStatus.OK;
            case BAD_REQUEST, VALIDATION_ERROR, USERNAME_EXISTS, EMAIL_EXISTS, REGISTER_FAILED -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, INVALID_CREDENTIALS, USER_NOT_FOUND_OR_DISABLED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String extractValidationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manv) {
            if (!manv.getBindingResult().getAllErrors().isEmpty()) {
                return manv.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        }
        if (ex instanceof BindException be) {
            if (!be.getBindingResult().getAllErrors().isEmpty()) {
                return be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        }
        if (ex instanceof ConstraintViolationException cve) {
            return cve.getConstraintViolations().stream().findFirst().map(v -> v.getMessage()).orElse(ResultCode.VALIDATION_ERROR.defaultMessage());
        }
        return ResultCode.VALIDATION_ERROR.defaultMessage();
    }
}
