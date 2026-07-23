package com.promsearch.prompt.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워터마크 처리가 완료된 프롬프트 결과 이미지")
public record PromptImageResponse(
        @Schema(description = "이미지 ID", example = "31")
        Long imageId,

        @Schema(
                description = "워터마크 이미지의 Presigned URL. 원본 이미지 경로는 제공하지 않습니다.",
                example = "https://storage.example.com/final/prompts/10/31.webp?X-Amz-Signature=..."
        )
        String imageUrl,

        @Schema(description = "이미지 정렬 순서", example = "0")
        int sortOrder,

        @Schema(description = "썸네일 여부", example = "true")
        boolean thumbnail
) {
}
