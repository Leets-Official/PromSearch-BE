package com.promsearch.community.interfaces.dto.response;

import com.promsearch.community.application.usecase.dto.LikeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record LikeResponse(
        @Schema(description = "프롬프트 식별자", example = "10")
        Long promptId,

        @Schema(description = "요청 사용자 기준 좋아요 여부", example = "true")
        boolean liked,

        @Schema(description = "프롬프트의 현재 좋아요 수", example = "12")
        long likeCount
) {

    public static LikeResponse from(LikeInfo info) {
        return new LikeResponse(info.promptId(), info.liked(), info.likeCount());
    }
}
