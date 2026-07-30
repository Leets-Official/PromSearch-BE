package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.HomePromptAuthorInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 프롬프트 카드에 표시되는 작성자 정보")
public record HomePromptAuthorResponse(
        @Schema(description = "작성자 사용자 ID", example = "12")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "prompt-maker")
        String nickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.promsearch.com/profiles/12.jpg", nullable = true)
        String profileImageUrl
) {

    public static HomePromptAuthorResponse from(HomePromptAuthorInfo info) {
        return new HomePromptAuthorResponse(info.userId(), info.nickname(), info.profileImageUrl());
    }
}
