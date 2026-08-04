package com.promsearch.commerce.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum CommerceErrorCode implements BaseCode {

    POINT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMERCE-001", "포인트 내역을 찾을 수 없습니다."),
    POST_UNLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMERCE-002", "프롬프트 열람 권한을 찾을 수 없습니다."),
    ALREADY_UNLOCKED(HttpStatus.CONFLICT, "COMMERCE-003", "이미 열람 권한을 획득한 프롬프트입니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "COMMERCE-004", "포인트가 부족합니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "COMMERCE-005", "포인트 변동 값이 유효하지 않습니다."),
    INVALID_POINT_TRANSACTION_TYPE(HttpStatus.BAD_REQUEST, "COMMERCE-006", "포인트 변동 유형이 유효하지 않습니다."),
    INVALID_POINT_REFERENCE(HttpStatus.BAD_REQUEST, "COMMERCE-007", "포인트 참조 대상이 유효하지 않습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "COMMERCE-008", "식별자가 유효하지 않습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "COMMERCE-009", "사용자 식별자가 유효하지 않습니다."),
    INVALID_POST_ID(HttpStatus.BAD_REQUEST, "COMMERCE-010", "프롬프트 식별자가 유효하지 않습니다."),
    PROMPT_NOT_ACCESSIBLE(HttpStatus.NOT_FOUND, "COMMERCE-011", "접근 가능한 프롬프트를 찾을 수 없습니다."),
    PAID_PROMPT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMERCE-012", "유료 프롬프트 복사 권한이 없습니다."),
    COPY_COUNT_UPDATE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMERCE-013",
            "프롬프트 복사 횟수 갱신에 실패했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CommerceErrorCode(HttpStatus httpStatus, String code, String message) {
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
