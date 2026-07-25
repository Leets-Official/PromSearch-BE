package com.promsearch.auth.application.port.out.token;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;

public interface AccessTokenProvider {

    String createAccessToken(AuthenticatedUserInfo user);

    AccessTokenClaims parseAccessToken(String accessToken);

    Long getAccessTokenExpirationSeconds();

    record AccessTokenClaims(Long userId, String role) {
    }
}
