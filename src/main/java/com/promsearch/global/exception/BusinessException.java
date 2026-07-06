package com.promsearch.global.exception;

import com.promsearch.global.exception.constant.Domain;
import com.promsearch.global.response.code.BaseCode;

public abstract class BusinessException extends RuntimeException {

    private final Domain domain;
    private final BaseCode baseCode;

    protected BusinessException(Domain domain, BaseCode baseCode) {
        super(baseCode.getMessage());
        this.domain = domain;
        this.baseCode = baseCode;
    }

    protected BusinessException(Domain domain, BaseCode baseCode, String message) {
        super(message != null ? message : baseCode.getMessage());
        this.domain = domain;
        this.baseCode = baseCode;
    }

    public Domain getDomain() {
        return domain;
    }

    public BaseCode getBaseCode() {
        return baseCode;
    }
}
