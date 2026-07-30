package com.promsearch.community.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum CommunityErrorCode implements BaseCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-001", "댓글을 찾을 수 없습니다."),
    INVALID_COMMENT_CONTENT(HttpStatus.BAD_REQUEST, "COMMUNITY-002", "댓글 내용이 유효하지 않습니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "COMMUNITY-003", "부모 댓글 정보가 유효하지 않습니다."),
    COMMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "COMMUNITY-004", "본인의 댓글만 수정/삭제할 수 있습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "COMMUNITY-005", "이미 삭제된 댓글입니다."),
    INTERACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-006", "상호작용 정보를 찾을 수 없습니다."),
    INVALID_INTERACTION_TYPE(HttpStatus.BAD_REQUEST, "COMMUNITY-007", "상호작용 종류가 유효하지 않습니다."),
    ALREADY_INTERACTED(HttpStatus.CONFLICT, "COMMUNITY-008", "이미 처리된 상호작용입니다."),
    INTERACTION_NOT_OWNED(HttpStatus.FORBIDDEN, "COMMUNITY-009", "본인의 상호작용만 취소할 수 있습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-010", "식별자가 유효하지 않습니다."),
    INVALID_COMMENT_POST_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-011", "댓글의 프롬프트 식별자가 유효하지 않습니다."),
    INVALID_COMMENT_USER_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-012", "댓글 작성자 식별자가 유효하지 않습니다."),
    INVALID_INTERACTION_POST_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-013", "상호작용 프롬프트 식별자가 유효하지 않습니다."),
    INVALID_INTERACTION_USER_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-014", "상호작용 사용자 식별자가 유효하지 않습니다."),
    INTERACTION_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-015", "상호작용할 수 있는 프롬프트를 찾을 수 없습니다."),
    INTERACTION_COUNT_INCONSISTENT(HttpStatus.CONFLICT, "COMMUNITY-016", "프롬프트 상호작용 수가 올바르지 않습니다."),
    INVALID_BOOKMARK_PAGE(HttpStatus.BAD_REQUEST, "COMMUNITY-017", "북마크 페이지 정보가 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CommunityErrorCode(HttpStatus httpStatus, String code, String message) {
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
