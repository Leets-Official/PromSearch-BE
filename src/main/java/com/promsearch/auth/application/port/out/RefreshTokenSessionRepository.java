package com.promsearch.auth.application.port.out;

import com.promsearch.auth.domain.RefreshTokenSession;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenSessionRepository {

    RefreshTokenSession save(RefreshTokenSession session);

    Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash);

    void revokeFamily(String familyId, Instant revokedAt);
}
