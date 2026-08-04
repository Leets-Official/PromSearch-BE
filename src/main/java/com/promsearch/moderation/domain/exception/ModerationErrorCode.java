package com.promsearch.moderation.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum ModerationErrorCode implements BaseCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "MODERATION-001", "신고 정보를 찾을 수 없습니다."),
    INVALID_REPORT_REASON(HttpStatus.BAD_REQUEST, "MODERATION-002", "신고 사유가 유효하지 않습니다."),
    INVALID_REPORT_DESCRIPTION(HttpStatus.BAD_REQUEST, "MODERATION-003", "신고 설명이 유효하지 않습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "MODERATION-004", "이미 신고한 대상입니다."),
    INVALID_REPORT_STATUS(HttpStatus.BAD_REQUEST, "MODERATION-005", "신고 처리 상태가 유효하지 않습니다."),
    REPORT_NOT_OWNED(HttpStatus.FORBIDDEN, "MODERATION-006", "본인의 신고만 조회할 수 있습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "MODERATION-007", "신고 식별자가 유효하지 않습니다."),
    INVALID_REPORTER_ID(HttpStatus.BAD_REQUEST, "MODERATION-008", "신고자 식별자가 유효하지 않습니다."),
    INVALID_POST_ID(HttpStatus.BAD_REQUEST, "MODERATION-009", "게시글 ID가 유효하지 않습니다."),
    INVALID_COMMENT_ID(HttpStatus.BAD_REQUEST, "MODERATION-010", "댓글 ID가 유효하지 않습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "MODERATION-011", "신고 대상을 찾을 수 없습니다."),
    REPORT_COUNT_UPDATE_FAILED(HttpStatus.CONFLICT, "MODERATION-012", "게시글 신고 수를 갱신할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ModerationErrorCode(HttpStatus httpStatus, String code, String message) {
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
