package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "내 게시완료 목록 카드에 필요한 요약 정보. 프롬프트 본문은 포함하지 않습니다.")
public record MyPromptSummaryResponse(
        @Schema(description = "프롬프트 식별자", example = "1")
        Long promptId,
        @Schema(description = "제목", example = "금융 앱 온보딩 UI", maxLength = 20)
        String title,
        @Schema(description = "게시 완료 시각", example = "2026-07-23T12:00:00Z")
        Instant publishedAt,
        @Schema(description = "누적 조회수", example = "128")
        long viewCount,
        @Schema(description = "누적 추천수", example = "12")
        long recommendCount
) {
}
