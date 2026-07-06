package com.promsearch.tracking.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum TrackingErrorCode implements BaseCode {

    EVENT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKING-001", "이벤트 로그를 찾을 수 없습니다."),
    INVALID_EVENT_NAME(HttpStatus.BAD_REQUEST, "TRACKING-002", "이벤트명이 유효하지 않습니다."),
    INVALID_SESSION_ID(HttpStatus.BAD_REQUEST, "TRACKING-003", "세션 식별자가 유효하지 않습니다."),
    INVALID_TARGET_TYPE(HttpStatus.BAD_REQUEST, "TRACKING-004", "이벤트 대상 종류가 유효하지 않습니다."),
    INVALID_EVENT_PROPERTIES(HttpStatus.BAD_REQUEST, "TRACKING-005", "이벤트 속성 데이터가 유효하지 않습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "TRACKING-006", "이벤트 로그 식별자가 유효하지 않습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "TRACKING-007", "이벤트 사용자 식별자가 유효하지 않습니다."),
    INVALID_TARGET_ID(HttpStatus.BAD_REQUEST, "TRACKING-008", "이벤트 대상 식별자가 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TrackingErrorCode(HttpStatus httpStatus, String code, String message) {
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
