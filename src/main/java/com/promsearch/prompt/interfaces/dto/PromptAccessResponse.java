package com.promsearch.prompt.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 사용자 기준 프롬프트 본문 접근 상태")
public record PromptAccessResponse(
        @Schema(description = "원문 전체가 잠겨 있는지 여부", example = "true")
        boolean locked,

        @Schema(
                description = "잠금 사유. 원문 전체를 조회할 수 있으면 null입니다.",
                example = "PREMIUM",
                nullable = true
        )
        PromptAccessReason reason
) {
}
