package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptStatisticsInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 프롬프트 카드에 표시되는 공개 통계")
public record HomePromptStatisticsResponse(
        @Schema(description = "조회 수", example = "120")
        long viewCount,

        @Schema(description = "좋아요 수. 인기 목록 정렬 기준입니다.", example = "32")
        long likeCount,

        @Schema(description = "댓글 수", example = "7")
        long commentCount,

        @Schema(description = "복사 수", example = "15")
        long copyCount
) {

    public static HomePromptStatisticsResponse from(HomePromptStatisticsInfo info) {
        return new HomePromptStatisticsResponse(
                info.viewCount(),
                info.likeCount(),
                info.commentCount(),
                info.copyCount()
        );
    }
}
