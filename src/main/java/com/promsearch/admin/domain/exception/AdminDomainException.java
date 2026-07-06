package com.promsearch.admin.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class AdminDomainException extends BusinessException {

    public AdminDomainException(AdminErrorCode errorCode) {
        super(Domain.ADMIN, errorCode);
    }

    public AdminDomainException(AdminErrorCode errorCode, String message) {
        super(Domain.ADMIN, errorCode, message);
    }
}
