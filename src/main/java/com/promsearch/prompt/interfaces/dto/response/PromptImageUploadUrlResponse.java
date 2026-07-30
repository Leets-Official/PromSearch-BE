package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "S3 임시 이미지 업로드 URL 발급 응답")
public record PromptImageUploadUrlResponse(
        @Schema(description = "요청 순서대로 발급된 이미지 업로드 정보")
        List<UploadTarget> images
) {

    public static PromptImageUploadUrlResponse from(PromptImageUploadUrlsInfo info) {
        return new PromptImageUploadUrlResponse(
                info.images().stream()
                        .map(image -> new UploadTarget(
                                image.imageId(),
                                image.uploadUrl(),
                                image.expiresAt()
                        ))
                        .toList()
        );
    }

    @Schema(description = "개별 이미지 업로드 정보")
    public record UploadTarget(
            @Schema(description = "프롬프트 저장 요청에 전달할 이미지 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID imageId,

            @Schema(description = "S3 임시 영역 업로드용 Presigned URL", example = "https://example-bucket.s3.amazonaws.com/temp/image.jpg?signature=...")
            URI uploadUrl,

            @Schema(description = "업로드 URL 만료 시각", example = "2026-07-23T12:10:00Z")
            Instant expiresAt
    ) {
    }
}
