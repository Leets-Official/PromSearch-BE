package com.promsearch.community.application.usecase.dto;

public record DeleteCommentCommand(
        Long commentId,
        Long userId
) {
    public static DeleteCommentCommand of(Long commentId, Long userId) {
        return new DeleteCommentCommand(commentId, userId);
    }
}
