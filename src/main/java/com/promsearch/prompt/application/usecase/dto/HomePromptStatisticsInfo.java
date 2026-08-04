package com.promsearch.prompt.application.usecase.dto;

public record HomePromptStatisticsInfo(
        long viewCount,
        long likeCount,
        long commentCount,
        long copyCount
) {
}
