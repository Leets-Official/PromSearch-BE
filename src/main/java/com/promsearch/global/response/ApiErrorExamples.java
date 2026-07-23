package com.promsearch.global.response;

public final class ApiErrorExamples {

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

    private ApiErrorExamples() {
    }
}
