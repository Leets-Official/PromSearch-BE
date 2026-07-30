package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;

public interface HomePromptReader {

    HomePromptListInfo listPopularPrompts(HomePromptListQuery query);

    HomePromptListInfo listJobPrompts(HomePromptListQuery query);
}
