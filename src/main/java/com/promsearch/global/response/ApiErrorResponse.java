package com.promsearch.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 오류 응답")
public record ApiErrorResponse(
        @Schema(description = "요청 성공 여부", example = "false")
        boolean success,

        @Schema(description = "오류 코드", example = "COMMON-4XX")
        String code,

        @Schema(description = "오류 메시지", example = "요청 처리 중 오류가 발생했습니다.")
        String message,

        @Schema(description = "오류 상세 정보. 상세 정보가 없으면 응답에서 생략됩니다.", nullable = true)
        Object result
) {
}
