package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.Prompt;
import java.util.List;

public record PromptPageResult(List<Prompt> content, long totalElements) {

    public PromptPageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
