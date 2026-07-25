package com.promsearch.auth.application.port.out.token;

public interface VerifyAccessTokenPort {

    AccessTokenClaims verifyAccessToken(String accessToken);
}
