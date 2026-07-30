package com.promsearch.prompt.application.usecase.dto;

import java.util.List;

public record HomePromptListInfo(
        List<HomePromptSummaryInfo> prompts,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
