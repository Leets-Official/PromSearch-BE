package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;

public interface ListMyPromptsUseCase {

    MyPromptPageInfo listMyPrompts(ListMyPromptsQuery query);
}
