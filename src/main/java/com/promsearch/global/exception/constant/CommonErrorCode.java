package com.promsearch.global.exception.constant;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements BaseCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "알 수 없는 오류입니다. 관리자에게 문의해주세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-403", "허용되지 않는 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "지원하지 않는 HTTP 메서드입니다."),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "COMMON-501", "아직 구현되지 않은 기능입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 유효하지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON-002", "요청 본문 형식이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CommonErrorCode(HttpStatus httpStatus, String code, String message) {
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

    public static final class Examples {

        public static final String BAD_REQUEST = """
                {
                  "success": false,
                  "code": "COMMON-400",
                  "message": "잘못된 요청입니다."
                }
                """;

        public static final String UNAUTHORIZED = """
                {
                  "success": false,
                  "code": "COMMON-401",
                  "message": "인증이 필요합니다."
                }
                """;

        public static final String FORBIDDEN = """
                {
                  "success": false,
                  "code": "COMMON-403",
                  "message": "허용되지 않는 요청입니다."
                }
                """;

        public static final String NOT_FOUND = """
                {
                  "success": false,
                  "code": "COMMON-404",
                  "message": "요청한 리소스를 찾을 수 없습니다."
                }
                """;

        private Examples() {
        }
    }
}
