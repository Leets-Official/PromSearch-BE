package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptInsightInfo;

public interface GetMyPromptInsightsUseCase {

    PromptInsightInfo getMyPromptInsights(Long userId);
}
