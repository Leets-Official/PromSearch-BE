package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "프롬프트에 연결된 이미지")
public record PromptImageResponse(
        @Schema(description = "이미지 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID imageId,

        @Schema(description = "이미지 노출 순서", example = "0")
        int sortOrder,

        @Schema(description = "카드 썸네일 사용 여부", example = "true")
        boolean thumbnail
) {
}
