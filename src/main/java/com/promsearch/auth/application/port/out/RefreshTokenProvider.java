package com.promsearch.auth.application.port.out;

import com.promsearch.auth.application.AuthenticatedUserInfo;
import java.time.Instant;

public interface RefreshTokenProvider {

    RefreshToken createRefreshToken(AuthenticatedUserInfo user);

    RefreshTokenClaims parse(String refreshToken);

    record RefreshToken(String value, Instant expiresAt) {
    }

    record RefreshTokenClaims(Long userId, String jti, Instant expiresAt) {
    }
}
