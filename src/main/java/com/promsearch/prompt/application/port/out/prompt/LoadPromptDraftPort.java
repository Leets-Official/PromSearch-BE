package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import java.util.Optional;

public interface LoadPromptDraftPort {

    Optional<Long> findDraftPromptIdByUserId(Long userId);

    Optional<PromptDraftInfo> findDraftByUserId(Long userId);
}
