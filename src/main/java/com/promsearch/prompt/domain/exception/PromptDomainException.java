package com.promsearch.prompt.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class PromptDomainException extends BusinessException {

    public PromptDomainException(PromptErrorCode errorCode) {
        super(Domain.PROMPT, errorCode);
    }

    public PromptDomainException(PromptErrorCode errorCode, String message) {
        super(Domain.PROMPT, errorCode, message);
    }

    public PromptDomainException(
            PromptErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(Domain.PROMPT, errorCode, message);
        initCause(cause);
    }
}
