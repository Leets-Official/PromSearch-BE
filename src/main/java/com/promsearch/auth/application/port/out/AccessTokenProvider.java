package com.promsearch.auth.application.port.out;

import com.promsearch.auth.application.AuthenticatedUserInfo;

public interface AccessTokenProvider {

    String createAccessToken(AuthenticatedUserInfo user);

    AccessTokenClaims parseAccessToken(String accessToken);

    Long getAccessTokenExpirationSeconds();

    record AccessTokenClaims(Long userId, String role) {
    }
}
