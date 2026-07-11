package com.promsearch.auth.application.port.out;

import com.promsearch.auth.domain.RefreshTokenSession;
import java.util.Optional;

public interface RefreshTokenSessionRepository {

    RefreshTokenSession save(RefreshTokenSession session);

    Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash);
}
