package com.promsearch.auth.application.port.out.persistence.refresh;

import com.promsearch.auth.domain.RefreshTokenSession;
import java.util.Optional;

public interface LoadRefreshTokenSessionPort {

    Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash);
}
