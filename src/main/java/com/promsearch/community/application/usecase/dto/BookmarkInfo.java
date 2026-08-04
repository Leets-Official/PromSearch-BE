package com.promsearch.community.application.usecase.dto;

import java.time.Instant;

public record BookmarkInfo(
        boolean bookmarked,
        Instant bookmarkedAt
) {
}
