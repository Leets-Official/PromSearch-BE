package com.promsearch.prompt.application.usecase.dto;

public record HomePromptAuthorInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
