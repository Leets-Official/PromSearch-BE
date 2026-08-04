package com.promsearch.commerce.application.usecase.dto;

import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;

public record UnlockPromptCommand(Long userId, Long promptId) {

    public UnlockPromptCommand {
        validateIds(userId, promptId);
    }

    private static void validateIds(Long userId, Long promptId) {
        if (userId == null || userId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_USER_ID);
        }
        if (promptId == null || promptId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POST_ID);
        }
    }
}
