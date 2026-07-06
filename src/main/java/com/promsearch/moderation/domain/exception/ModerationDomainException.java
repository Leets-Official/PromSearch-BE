package com.promsearch.moderation.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class ModerationDomainException extends BusinessException {

    public ModerationDomainException(ModerationErrorCode errorCode) {
        super(Domain.MODERATION, errorCode);
    }

    public ModerationDomainException(ModerationErrorCode errorCode, String message) {
        super(Domain.MODERATION, errorCode, message);
    }
}
