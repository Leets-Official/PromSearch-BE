package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;

public interface ListHomePromptsUseCase {

    HomePromptListInfo listPrompts(HomePromptListQuery query);

    HomePromptListInfo listPopularPrompts(HomePromptListQuery query);

    HomePromptListInfo listJobPrompts(HomePromptListQuery query);
}
