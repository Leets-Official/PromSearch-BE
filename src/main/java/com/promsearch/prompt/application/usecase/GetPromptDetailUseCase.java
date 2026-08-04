package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo;

public interface GetPromptDetailUseCase {
    PromptDetailInfo get(Long promptId, Long viewerId);
}
