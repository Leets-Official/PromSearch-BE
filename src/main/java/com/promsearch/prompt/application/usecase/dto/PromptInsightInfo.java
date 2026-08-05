package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.application.port.out.prompt.PromptInsightTotals;

public record PromptInsightInfo(long totalViews, long totalRecommends, long totalCopies) {

    public static PromptInsightInfo from(PromptInsightTotals totals) {
        return new PromptInsightInfo(totals.totalViews(), totals.totalRecommends(), totals.totalCopies());
    }
}
