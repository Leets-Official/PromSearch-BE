package com.promsearch.community.application.usecase.dto;

public record GetCommentRepliesQuery(
        Long parentCommentId,
        Long viewerId,
        Long cursor,
        int size
) {
    public static GetCommentRepliesQuery of(
            Long parentCommentId,
            Long viewerId,
            Long cursor,
            int size
    ) {
        return new GetCommentRepliesQuery(parentCommentId, viewerId, cursor, size);
    }
}
