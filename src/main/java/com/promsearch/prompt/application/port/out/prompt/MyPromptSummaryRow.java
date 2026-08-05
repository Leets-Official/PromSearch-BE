package com.promsearch.prompt.application.port.out.prompt;

import java.time.Instant;

public record MyPromptSummaryRow(
        Long promptId,
        String title,
        Instant publishedAt,
        long viewCount,
        long recommendCount
) {
}
