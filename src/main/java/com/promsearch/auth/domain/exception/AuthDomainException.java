package com.promsearch.auth.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class AuthDomainException extends BusinessException {

    public AuthDomainException(AuthErrorCode errorCode) {
        super(Domain.AUTH, errorCode);
    }

    public AuthDomainException(AuthErrorCode errorCode, String message) {
        super(Domain.AUTH, errorCode, message);
    }
}
