package com.promsearch.auth.application.port.out.refresh;

import com.promsearch.auth.domain.RefreshTokenSession;
import java.time.Instant;

public interface SaveRefreshTokenSessionPort {

    RefreshTokenSession save(RefreshTokenSession session);

    void revokeFamily(String familyId, Instant revokedAt);
}
