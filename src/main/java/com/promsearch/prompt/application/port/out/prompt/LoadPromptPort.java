package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.enums.PromptStatus;

public interface LoadPromptPort {

    PromptPageResult listByUserIdAndStatus(Long userId, PromptStatus status, int page, int size);

    PromptInsightTotals sumInsightsByUserId(Long userId);
}
