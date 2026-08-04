package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.LikeInfo;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;

public interface LikePromptUseCase {

    LikeInfo like(LikePromptCommand command);
}
