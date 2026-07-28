package com.promsearch.community.application.usecase.dto;

public record GetCommentsQuery(
        Long postId,
        Long viewerId
) {
    public static GetCommentsQuery of(Long postId, Long viewerId) {
        return new GetCommentsQuery(postId, viewerId);
    }
}
