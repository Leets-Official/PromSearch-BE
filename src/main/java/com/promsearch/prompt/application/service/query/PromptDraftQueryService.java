package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptDraftPort;
import com.promsearch.prompt.application.usecase.GetPromptDraftUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptDraftQueryService implements GetPromptDraftUseCase {

    private final LoadPromptDraftPort loadPromptDraftPort;

    @Override
    public PromptDraftInfo get(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        return loadPromptDraftPort.findDraftByUserId(userId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));
    }
}
