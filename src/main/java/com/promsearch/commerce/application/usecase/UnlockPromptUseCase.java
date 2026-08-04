package com.promsearch.commerce.application.usecase;

import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;

public interface UnlockPromptUseCase {

    void unlock(UnlockPromptCommand command);
}
