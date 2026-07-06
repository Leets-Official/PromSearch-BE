package com.promsearch.tracking.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class TrackingDomainException extends BusinessException {

    public TrackingDomainException(TrackingErrorCode errorCode) {
        super(Domain.TRACKING, errorCode);
    }

    public TrackingDomainException(TrackingErrorCode errorCode, String message) {
        super(Domain.TRACKING, errorCode, message);
    }
}
