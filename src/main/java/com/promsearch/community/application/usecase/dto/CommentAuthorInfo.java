package com.promsearch.community.application.usecase.dto;

public record CommentAuthorInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
