package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
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

        @Schema(description = "제목", example = "회의록 자동 정리", maxLength = Prompt.MAX_TITLE_LENGTH)
        String title,

        @Schema(description = "프롬프트 설명")
        String description,

        @Schema(description = "프롬프트 실행 결과 타입", example = "TEXT")
        PromptOutputType outputType,

        @Schema(description = "선택한 직군 태그 식별자 목록", example = "[1, 2]")
        List<Long> jobTagIds,

        @Schema(description = "선택한 태스크 태그 식별자 목록", example = "[10, 11]")
        List<Long> taskTagIds,

        @Schema(description = "선택한 AI 모델 태그 식별자", example = "20", nullable = true)
        Long aiModelTagId,

        @Schema(description = "AI 모델 '기타' 직접 입력 원문", example = "GPT 4.1 Mini")
        String customAiModel,

        @Schema(description = "콘텐츠 타입", example = "FREE")
        PromptContentType contentType,

        @Schema(description = "프롬프트 본문")
        String promptBody,

        @Schema(description = "작성자가 설정한 공개 범위", example = "PUBLIC")
        PromptVisibility visibility,

        @Schema(description = "연결된 이미지 목록")
        List<PromptImageResponse> images,

        @Schema(description = "게시물 처리 상태. 임시저장은 DRAFT입니다.", example = "DRAFT")
        PromptStatus status,

        @Schema(description = "서버에서 결정한 가격", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
        Long pricePoint,

        @Schema(description = "마지막 임시저장 시각", example = "2026-07-23T21:00:00+09:00")
        Instant updatedAt
) {

    public static PromptDraftResponse from(PromptDraftInfo info) {
        return new PromptDraftResponse(
                info.promptId(),
                info.title(),
                info.description(),
                info.outputType(),
                info.jobTagIds(),
                info.taskTagIds(),
                info.aiModelTagId(),
                info.customAiModel(),
                info.contentType(),
                info.promptBody(),
                info.visibility(),
                info.images().stream()
                        .map(image -> new PromptImageResponse(
                                image.imageId(),
                                image.sortOrder(),
                                image.thumbnail()
                        ))
                        .toList(),
                info.status(),
                info.pricePoint(),
                info.updatedAt()
        );
    }
}
