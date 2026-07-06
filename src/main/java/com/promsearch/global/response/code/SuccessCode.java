package com.promsearch.global.response.code;

import org.springframework.http.HttpStatus;

public enum SuccessCode implements BaseCode {

    OK(HttpStatus.OK, "COMMON-200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON-201", "생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON-204", "성공적으로 처리되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus httpStatus, String code, String message) {
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
