package com.promsearch.auth.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements BaseCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "로그인이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-003", "액세스 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-005", "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
