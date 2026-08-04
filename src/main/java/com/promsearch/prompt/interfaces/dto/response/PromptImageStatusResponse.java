package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptImageStatusInfo;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "프롬프트 이미지 처리 상태")
public record PromptImageStatusResponse(
        @Schema(description = "이미지 자산 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID imageId,

        @Schema(description = "이미지 자산 상태", example = "PROCESSING")
        PromptImageStatus status,

        @Schema(description = "워터마크 처리 실패 코드. 실패 상태가 아니면 null", example = "WATERMARK_RENDER_FAILED")
        String failureCode,

        @Schema(description = "READY 상태의 워터마크 완료 이미지 Presigned 조회 URL. 그 외 상태에서는 null", example = "https://storage.example.com/watermarked/image.jpg?X-Amz-Signature=...")
        String imageUrl
) {

    public static PromptImageStatusResponse from(PromptImageStatusInfo info) {
        return new PromptImageStatusResponse(
                info.imageId(),
                info.status(),
                info.failureCode(),
                info.imageUrl()
        );
    }
}
