package com.promsearch.auth.application.port.out.token;

import java.time.Instant;

public record IssuedRefreshToken(
        String value,
        Instant expiresAt
) {
}
