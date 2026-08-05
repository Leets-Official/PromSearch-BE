package com.promsearch.prompt.application.port.out.prompt;

import java.util.List;

public record PromptPageResult(List<MyPromptSummaryRow> content, long totalElements) {

    public PromptPageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
