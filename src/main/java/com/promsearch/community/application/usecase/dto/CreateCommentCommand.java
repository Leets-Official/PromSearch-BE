package com.promsearch.community.application.usecase.dto;

public record CreateCommentCommand(
        Long postId,
        Long userId,
        String content
) {
    public static CreateCommentCommand of(Long postId, Long userId, String content) {
        return new CreateCommentCommand(postId, userId, content);
    }
}
