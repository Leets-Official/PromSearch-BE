package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptAuthorInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 카드 작성자 정보")
public record HomePromptAuthorResponse(
        @Schema(description = "작성자 ID", example = "12")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "프롬프트장인")
        String nickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.promsearch.com/profiles/12.jpg", nullable = true)
        String profileImageUrl
) {

    public static HomePromptAuthorResponse from(HomePromptAuthorInfo info) {
        return new HomePromptAuthorResponse(info.userId(), info.nickname(), info.profileImageUrl());
    }
}
