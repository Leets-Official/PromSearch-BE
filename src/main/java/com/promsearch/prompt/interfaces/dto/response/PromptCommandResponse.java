package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "프롬프트 생성 또는 임시저장 결과")
public record PromptCommandResponse(
        @Schema(description = "프롬프트 식별자", example = "1")
        Long promptId,

        @Schema(description = "게시물 처리 상태. 작성자 공개 범위와 별개입니다.", example = "ACTIVE")
        PromptStatus status,

        @Schema(description = "작성자가 설정한 공개 범위", example = "PUBLIC")
        PromptVisibility visibility,

        @Schema(description = "서버에서 결정한 가격. FREE는 0이며 PREMIUM 실제 가격은 추후 설정합니다.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
        Long pricePoint,

        @Schema(description = "마지막 저장 시각", example = "2026-07-23T12:00:00Z")
        Instant updatedAt
) {
}
