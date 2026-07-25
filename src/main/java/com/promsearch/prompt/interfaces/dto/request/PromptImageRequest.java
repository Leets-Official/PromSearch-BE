package com.promsearch.prompt.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "프롬프트에 연결할 업로드 완료 이미지")
public record PromptImageRequest(
        @Schema(description = "업로드 URL 발급 시 받은 이미지 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "imageId must not be null")
        UUID imageId,

        @Schema(description = "이미지 노출 순서", example = "0", minimum = "0")
        @Min(value = 0, message = "sortOrder must be 0 or greater")
        int sortOrder,

        @Schema(description = "카드 썸네일 사용 여부. 이미지 전체에서 최대 한 장만 true로 지정합니다.", example = "true")
        boolean thumbnail
) {
}
