package com.promsearch.global.exception;

import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.exception.constant.Domain;

/**
 * Controller에서 설계만 완료되었고, 아직 구현되지 않은 경우 해당 에러를 발생시켜야 합니다.
 */
public class NotImplementedException extends BusinessException {

    public NotImplementedException() {
        super(Domain.COMMON, CommonErrorCode.NOT_IMPLEMENTED);
    }

    public NotImplementedException(String message) {
        super(Domain.COMMON, CommonErrorCode.NOT_IMPLEMENTED, message);
    }
}
