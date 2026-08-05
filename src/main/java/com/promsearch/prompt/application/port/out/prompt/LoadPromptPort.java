package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;

public interface LoadPromptPort {

    PromptPageResult listByUserIdAndStatus(
            Long userId,
            PromptStatus status,
            PromptVisibility visibility,
            int page,
            int size
    );

    PromptInsightTotals sumInsightsByUserId(Long userId);
}
