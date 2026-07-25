package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 사용자의 프롬프트 상호작용 상태")
public record PromptViewerInteractionResponse(
        @Schema(description = "로그인 사용자가 해당 프롬프트에 좋아요를 눌렀는지 여부", example = "true")
        boolean liked,

        @Schema(description = "로그인 사용자가 해당 프롬프트를 북마크했는지 여부", example = "false")
        boolean bookmarked
) {
}
