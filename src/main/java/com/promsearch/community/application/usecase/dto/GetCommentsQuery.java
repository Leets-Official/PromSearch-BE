package com.promsearch.community.application.usecase.dto;

public record GetCommentsQuery(
        Long postId,
        Long viewerId,
        Long cursor,
        int size
) {
    public static GetCommentsQuery of(Long postId, Long viewerId) {
        return new GetCommentsQuery(postId, viewerId, null, 20);
    }

    public static GetCommentsQuery of(Long postId, Long viewerId, Long cursor, int size) {
        return new GetCommentsQuery(postId, viewerId, cursor, size);
    }
}
