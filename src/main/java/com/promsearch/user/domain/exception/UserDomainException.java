package com.promsearch.user.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class UserDomainException extends BusinessException {

    public UserDomainException(UserErrorCode errorCode) {
        super(Domain.USER, errorCode);
    }

    public UserDomainException(UserErrorCode errorCode, String message) {
        super(Domain.USER, errorCode, message);
    }
}
