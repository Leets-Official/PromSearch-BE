package com.promsearch.auth.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements BaseCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "로그인이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-003", "액세스 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-005", "접근 권한이 없습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH-006", "지원하지 않는 소셜 로그인 제공자입니다."),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-007", "소셜 로그인 인증에 실패했습니다."),
    OAUTH_EMAIL_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "AUTH-008", "소셜 계정에서 이메일 정보를 가져올 수 없습니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "AUTH-009", "이미 연동된 소셜 계정입니다."),
    OAUTH_PROVIDER_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AUTH-010", "소셜 로그인 제공자 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),
    OAUTH_PROVIDER_BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "AUTH-011", "소셜 로그인 제공자로부터 올바르지 않은 응답을 받았습니다."),
    OAUTH_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH-012", "소셜 로그인 제공자에 일시적으로 연결할 수 없습니다.");

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
