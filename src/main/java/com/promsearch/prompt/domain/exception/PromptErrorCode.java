package com.promsearch.prompt.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum PromptErrorCode implements BaseCode {

    PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-001", "프롬프트를 찾을 수 없습니다."),
    INVALID_PROMPT_TITLE(HttpStatus.BAD_REQUEST, "PROMPT-002", "프롬프트 제목이 유효하지 않습니다."),
    INVALID_PROMPT_BODY(HttpStatus.BAD_REQUEST, "PROMPT-003", "프롬프트 본문이 유효하지 않습니다."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-004", "콘텐츠 타입이 유효하지 않습니다."),
    INVALID_OUTPUT_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-005", "결과물 타입이 유효하지 않습니다."),
    INVALID_PRICE_POINT(HttpStatus.BAD_REQUEST, "PROMPT-006", "프롬프트 가격이 유효하지 않습니다."),
    PROMPT_NOT_OWNED(HttpStatus.FORBIDDEN, "PROMPT-007", "본인의 프롬프트만 수정/삭제할 수 있습니다."),
    PAID_PROMPT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROMPT-008", "유료 프롬프트 열람 권한이 없습니다."),
    PROMPT_ALREADY_DELETED(HttpStatus.CONFLICT, "PROMPT-009", "이미 삭제된 프롬프트입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-010", "태그를 찾을 수 없습니다."),
    INVALID_TAG_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-011", "태그 종류가 유효하지 않습니다."),
    INVALID_TAG_NAME(HttpStatus.BAD_REQUEST, "PROMPT-012", "태그명이 유효하지 않습니다."),
    DUPLICATE_TAG(HttpStatus.CONFLICT, "PROMPT-013", "이미 존재하는 태그입니다."),
    CUSTOM_TAG_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PROMPT-014", "해당 태그 종류는 사용자 생성이 허용되지 않습니다."),
    POST_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-015", "포스트 태그 정보를 찾을 수 없습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "PROMPT-016", "식별자가 유효하지 않습니다."),
    INVALID_PROMPT_USER_ID(HttpStatus.BAD_REQUEST, "PROMPT-017", "프롬프트 작성자 식별자가 유효하지 않습니다."),
    INVALID_PROMPT_STATUS(HttpStatus.BAD_REQUEST, "PROMPT-018", "프롬프트 상태가 유효하지 않습니다."),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "PROMPT-019", "이미지 주소가 유효하지 않습니다."),
    INVALID_IMAGE_ORDER(HttpStatus.BAD_REQUEST, "PROMPT-020", "이미지 순서가 유효하지 않습니다."),
    INVALID_TAG_ID(HttpStatus.BAD_REQUEST, "PROMPT-021", "태그 식별자가 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    PromptErrorCode(HttpStatus httpStatus, String code, String message) {
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
