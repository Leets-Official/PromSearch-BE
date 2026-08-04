package com.promsearch.user.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements BaseCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-003", "이미 사용 중인 닉네임입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "USER-004", "이메일이 유효하지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER-005", "비밀번호가 유효하지 않습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER-006", "닉네임이 유효하지 않습니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, "USER-007", "사용자 상태가 유효하지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "USER-008", "보유 포인트가 부족합니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "USER-009", "사용자 식별자가 유효하지 않습니다."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "USER-010", "이름이 유효하지 않습니다."),
    INVALID_USER_ROLE(HttpStatus.BAD_REQUEST, "USER-011", "사용자 권한이 유효하지 않습니다."),
    INVALID_USER_GRADE(HttpStatus.BAD_REQUEST, "USER-012", "사용자 등급이 유효하지 않습니다."),
    INVALID_POINT(HttpStatus.BAD_REQUEST, "USER-013", "보유 포인트가 유효하지 않습니다."),
    INVALID_INTEREST_TAG(HttpStatus.BAD_REQUEST, "USER-014", "관심 태그가 유효하지 않습니다."),
    INVALID_PROFILE_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "USER-015", "지원하지 않는 프로필 이미지 형식입니다."),
    INVALID_PROFILE_IMAGE_FILE_SIZE(HttpStatus.BAD_REQUEST, "USER-016", "프로필 이미지 파일 크기가 유효하지 않습니다."),
    PROFILE_IMAGE_NOT_OWNED(HttpStatus.FORBIDDEN, "USER-017", "다른 사용자의 프로필 이미지에는 접근할 수 없습니다."),
    PROFILE_IMAGE_UPLOAD_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER-018", "업로드된 프로필 이미지를 찾을 수 없습니다."),
    PROFILE_IMAGE_UPLOAD_METADATA_MISMATCH(HttpStatus.BAD_REQUEST, "USER-019", "업로드된 프로필 이미지 정보가 요청과 일치하지 않습니다."),
    PROFILE_IMAGE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "USER-020", "프로필 이미지 저장소를 사용할 수 없습니다."),
    GRADE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-021", "등급업 심사 대기 항목을 찾을 수 없습니다."),
    GRADE_REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "USER-022", "이미 처리된 등급업 심사 대기 항목입니다."),
    INVALID_GRADE_TRANSITION(HttpStatus.BAD_REQUEST, "USER-023", "Origin 등급은 Prime 등급에서만 승급할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String code, String message) {
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
