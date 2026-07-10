package com.promsearch.auth.application.port.out;

import com.promsearch.user.domain.User;
import java.time.Instant;

public interface RefreshTokenProvider {

    RefreshToken createRefreshToken(User user);

    RefreshTokenClaims parse(String refreshToken);

    record RefreshToken(String value, Instant expiresAt) {
    }

    record RefreshTokenClaims(Long userId, String jti, Instant expiresAt) {
    }
}
