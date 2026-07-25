package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptImageUploadInfo;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "프롬프트 이미지 업로드 완료 확인 결과")
public record PromptImageUploadCompleteResponse(
        @Schema(description = "이미지 자산 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID imageId,

        @Schema(description = "이미지 자산 상태", example = "UPLOADED")
        PromptImageStatus status,

        @Schema(description = "S3에 기록된 업로드 시각")
        Instant uploadedAt
) {

    public static PromptImageUploadCompleteResponse from(PromptImageUploadInfo info) {
        return new PromptImageUploadCompleteResponse(
                info.imageId(),
                info.status(),
                info.uploadedAt()
        );
    }
}
