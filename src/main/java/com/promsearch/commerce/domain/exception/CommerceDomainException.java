package com.promsearch.commerce.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class CommerceDomainException extends BusinessException {

    public CommerceDomainException(CommerceErrorCode errorCode) {
        super(Domain.COMMERCE, errorCode);
    }

    public CommerceDomainException(CommerceErrorCode errorCode, String message) {
        super(Domain.COMMERCE, errorCode, message);
    }
}
