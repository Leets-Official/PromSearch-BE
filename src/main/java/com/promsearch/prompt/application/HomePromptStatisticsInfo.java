package com.promsearch.prompt.application;

public record HomePromptStatisticsInfo(
        long viewCount,
        long likeCount,
        long commentCount,
        long copyCount
) {
}
