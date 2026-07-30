package com.promsearch.community.application.usecase.dto;

public record CreateCommentReplyCommand(
        Long parentCommentId,
        Long userId,
        String content
) {
    public static CreateCommentReplyCommand of(Long parentCommentId, Long userId, String content) {
        return new CreateCommentReplyCommand(parentCommentId, userId, content);
    }
}
