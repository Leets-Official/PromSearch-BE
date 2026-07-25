package com.promsearch.auth.application.port.out.token;

public interface VerifyRefreshTokenPort {

    RefreshTokenClaims verifyRefreshToken(String refreshToken);
}
