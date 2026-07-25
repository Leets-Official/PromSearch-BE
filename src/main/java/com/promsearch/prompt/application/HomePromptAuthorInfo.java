package com.promsearch.prompt.application;

public record HomePromptAuthorInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
