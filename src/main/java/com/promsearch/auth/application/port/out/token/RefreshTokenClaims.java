package com.promsearch.auth.application.port.out.token;

import java.time.Instant;

public record RefreshTokenClaims(
        Long userId,
        String jti,
        Instant expiresAt
) {
}
