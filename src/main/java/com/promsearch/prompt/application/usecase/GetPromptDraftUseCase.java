package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;

public interface GetPromptDraftUseCase {

    PromptDraftInfo get(Long userId);
}
