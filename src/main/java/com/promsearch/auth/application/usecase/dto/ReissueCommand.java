package com.promsearch.auth.application.usecase.dto;

import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;

public record ReissueCommand(
        String refreshToken
) {

    public static ReissueCommand of(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
        return new ReissueCommand(refreshToken);
    }
}
