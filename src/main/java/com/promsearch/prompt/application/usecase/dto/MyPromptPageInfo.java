package com.promsearch.prompt.application.usecase.dto;

import java.util.List;

public record MyPromptPageInfo(List<MyPromptSummaryInfo> content, long totalElements) {

    public MyPromptPageInfo {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
