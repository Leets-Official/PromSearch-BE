package com.promsearch.prompt.application;

import java.util.List;

public record HomePromptListInfo(
        List<HomePromptSummaryInfo> prompts,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
