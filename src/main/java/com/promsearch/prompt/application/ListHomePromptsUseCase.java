package com.promsearch.prompt.application;

public interface ListHomePromptsUseCase {

    HomePromptListInfo listPopularPrompts(HomePromptListQuery query);

    HomePromptListInfo listJobPrompts(HomePromptListQuery query);
}
