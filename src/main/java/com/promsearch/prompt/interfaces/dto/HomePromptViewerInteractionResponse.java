package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptViewerInteractionInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 조회자의 프롬프트 상호작용 상태")
public record HomePromptViewerInteractionResponse(
        @Schema(description = "현재 조회자가 이 프롬프트에 좋아요를 눌렀는지 여부", example = "true")
        boolean liked,

        @Schema(description = "현재 조회자가 이 프롬프트를 북마크했는지 여부", example = "false")
        boolean bookmarked
) {

    public static HomePromptViewerInteractionResponse from(HomePromptViewerInteractionInfo info) {
        return new HomePromptViewerInteractionResponse(info.liked(), info.bookmarked());
    }
}
