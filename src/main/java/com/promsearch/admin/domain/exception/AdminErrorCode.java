package com.promsearch.admin.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum AdminErrorCode implements BaseCode {

    ADMIN_AUTH_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN-001", "관리자 권한이 필요합니다."),
    INVALID_ADMIN_ACTION(HttpStatus.BAD_REQUEST, "ADMIN-002", "관리자 작업이 유효하지 않습니다."),
    TARGET_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN-003", "관리 대상 사용자를 찾을 수 없습니다."),
    TARGET_PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN-004", "관리 대상 프롬프트를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AdminErrorCode(HttpStatus httpStatus, String code, String message) {
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
