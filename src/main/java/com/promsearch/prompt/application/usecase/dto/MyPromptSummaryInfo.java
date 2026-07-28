package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.PostStatistics;
import com.promsearch.prompt.domain.Prompt;
import java.time.Instant;

public record MyPromptSummaryInfo(
        Long promptId,
        String title,
        Instant publishedAt,
        long viewCount,
        long recommendCount
) {

    public static MyPromptSummaryInfo from(Prompt prompt) {
        PostStatistics statistics = prompt.getStatistics();
        return new MyPromptSummaryInfo(
                prompt.getPromptId().id(),
                prompt.getTitle(),
                prompt.getPublishedAt(),
                statistics == null ? 0L : statistics.getViewCount(),
                statistics == null ? 0L : statistics.getLikeCount()
        );
    }
}
