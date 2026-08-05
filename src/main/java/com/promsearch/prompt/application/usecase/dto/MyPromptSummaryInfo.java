package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.application.port.out.prompt.MyPromptSummaryRow;
import java.time.Instant;

public record MyPromptSummaryInfo(
        Long promptId,
        String title,
        Instant publishedAt,
        long viewCount,
        long recommendCount
) {

    public static MyPromptSummaryInfo from(MyPromptSummaryRow row) {
        return new MyPromptSummaryInfo(
                row.promptId(),
                row.title(),
                row.publishedAt(),
                row.viewCount(),
                row.recommendCount()
        );
    }
}
