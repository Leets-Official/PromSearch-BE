package com.promsearch.auth.application.port.out.persistence.refresh;

import java.time.Instant;

public interface RevokeRefreshTokenSessionPort {

    void revokeActiveSessionsByUserId(Long userId, Instant revokedAt);
}
