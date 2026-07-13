package com.promsearch.auth.domain.enums;

import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;

public enum SocialProvider {

    KAKAO;

    public static SocialProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new AuthDomainException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        try {
            return SocialProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthDomainException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
