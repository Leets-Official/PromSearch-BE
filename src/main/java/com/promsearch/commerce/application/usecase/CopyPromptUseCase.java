package com.promsearch.commerce.application.usecase;

import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;

public interface CopyPromptUseCase {

    CopyPromptInfo copy(CopyPromptCommand command);
}
