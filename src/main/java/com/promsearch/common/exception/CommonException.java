package com.promsearch.common.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.exception.constant.Domain;

public class CommonException extends BusinessException {

    public CommonException(CommonErrorCode errorCode) {
        super(Domain.COMMON, errorCode);
    }

    public CommonException(CommonErrorCode errorCode, String message) {
        super(Domain.COMMON, errorCode, message);
    }
}
