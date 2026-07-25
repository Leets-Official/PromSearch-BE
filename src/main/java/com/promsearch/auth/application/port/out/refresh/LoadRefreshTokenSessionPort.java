package com.promsearch.auth.application.port.out.refresh;

import com.promsearch.auth.domain.RefreshTokenSession;
import java.util.Optional;

public interface LoadRefreshTokenSessionPort {

    Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash);
}
