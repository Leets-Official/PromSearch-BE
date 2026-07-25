package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptViewerInteractionInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 로그인 사용자의 프롬프트 상호작용 상태")
public record HomePromptViewerInteractionResponse(
        @Schema(description = "좋아요 여부", example = "true")
        boolean liked,

        @Schema(description = "북마크 여부", example = "false")
        boolean bookmarked
) {

    public static HomePromptViewerInteractionResponse from(HomePromptViewerInteractionInfo info) {
        return new HomePromptViewerInteractionResponse(info.liked(), info.bookmarked());
    }
}
