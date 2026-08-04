package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
import com.promsearch.prompt.application.usecase.dto.SavePromptDraftCommand;

public interface SavePromptDraftUseCase {

    PromptCommandInfo save(SavePromptDraftCommand command);
}
