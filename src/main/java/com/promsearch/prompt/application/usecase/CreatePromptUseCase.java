package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;

public interface CreatePromptUseCase {

    PromptCommandInfo create(CreatePromptCommand command);
}
