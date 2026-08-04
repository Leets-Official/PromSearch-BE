package com.promsearch.community.application.usecase.dto;

public record LikeInfo(
        Long promptId,
        boolean liked,
        long likeCount
) {
}
