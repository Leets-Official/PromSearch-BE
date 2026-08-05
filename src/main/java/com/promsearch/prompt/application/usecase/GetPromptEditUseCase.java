package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptEditInfo;

public interface GetPromptEditUseCase {

    PromptEditInfo get(Long promptId, Long userId);
}
