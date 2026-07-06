package com.promsearch.community.domain.exception;

import com.promsearch.global.exception.BusinessException;
import com.promsearch.global.exception.constant.Domain;

public class CommunityDomainException extends BusinessException {

    public CommunityDomainException(CommunityErrorCode errorCode) {
        super(Domain.COMMUNITY, errorCode);
    }

    public CommunityDomainException(CommunityErrorCode errorCode, String message) {
        super(Domain.COMMUNITY, errorCode, message);
    }
}
