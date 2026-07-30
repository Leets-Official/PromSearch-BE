package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 게시글 누적 인사이트. 논리 삭제된 게시물은 제외하고 실시간 합산(SUM)합니다.")
public record PromptInsightResponse(
        @Schema(description = "누적 조회수 합계", example = "1024")
        long totalViews,
        @Schema(description = "누적 추천수 합계", example = "88")
        long totalRecommends,
        @Schema(description = "누적 복사수 합계", example = "42")
        long totalCopies
) {
}
