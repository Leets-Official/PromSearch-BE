package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "인증된 사용자의 최신 임시저장 전체 내용")
public record PromptDraftResponse(
        @Schema(description = "프롬프트 식별자", example = "1")
        Long promptId,

        @Schema(description = "제목", example = "회의록 자동 정리", maxLength = 20)
        String title,

        @Schema(description = "프롬프트 설명")
        String description,

        @Schema(description = "프롬프트 실행 결과 타입", example = "TEXT")
        PromptOutputType outputType,

        @Schema(description = "선택한 직군 태그 식별자 목록", example = "[1, 2]")
        List<Long> jobTagIds,

        @Schema(description = "선택한 태스크 태그 식별자 목록", example = "[10, 11]")
        List<Long> taskTagIds,

        @Schema(description = "선택한 AI 모델 태그 식별자 목록", example = "[20]")
        List<Long> aiModelTagIds,

        @Schema(description = "AI 모델 '기타' 직접 입력 원문", example = "GPT 4.1 Mini")
        String customAiModel,

        @Schema(description = "콘텐츠 타입", example = "FREE")
        PromptContentType contentType,

        @Schema(description = "프롬프트 본문")
        String promptBody,

        @Schema(description = "작성자가 설정한 공개 범위", example = "PUBLIC")
        PromptVisibility visibility,

        @Schema(description = "연결된 이미지 목록")
        List<PromptImageRequest> images,

        @Schema(description = "게시물 처리 상태. 임시저장은 DRAFT입니다.", example = "DRAFT")
        PromptStatus status,

        @Schema(description = "서버에서 결정한 가격", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
        Long pricePoint,

        @Schema(description = "마지막 임시저장 시각", example = "2026-07-23T12:00:00Z")
        Instant updatedAt
) {
}
