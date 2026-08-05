package com.promsearch.commerce.application.usecase.dto;

import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;

public record CopyPromptCommand(Long userId, Long promptId) {

    public CopyPromptCommand {
        if (userId == null || userId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_USER_ID);
        }
        if (promptId == null || promptId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POST_ID);
        }
    }
}
