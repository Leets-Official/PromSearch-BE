package com.promsearch.community.application.usecase.dto;

public record UpdateCommentCommand(
        Long commentId,
        Long userId,
        String content
) {
    public static UpdateCommentCommand of(Long commentId, Long userId, String content) {
        return new UpdateCommentCommand(commentId, userId, content);
    }
}
