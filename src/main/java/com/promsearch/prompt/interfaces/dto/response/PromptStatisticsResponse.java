package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프롬프트 공개 통계")
public record PromptStatisticsResponse(
        @Schema(description = "조회 수", example = "120")
        long viewCount,

        @Schema(description = "복사 수", example = "15")
        long copyCount,

        @Schema(description = "좋아요 수", example = "32")
        long likeCount,

        @Schema(description = "댓글 수", example = "7")
        long commentCount
) {
}
