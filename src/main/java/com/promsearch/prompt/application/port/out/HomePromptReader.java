package com.promsearch.prompt.application.port.out;

import com.promsearch.prompt.application.HomePromptListInfo;
import com.promsearch.prompt.application.HomePromptListQuery;

public interface HomePromptReader {

    HomePromptListInfo listPopularPrompts(HomePromptListQuery query);

    HomePromptListInfo listJobPrompts(HomePromptListQuery query);
}
