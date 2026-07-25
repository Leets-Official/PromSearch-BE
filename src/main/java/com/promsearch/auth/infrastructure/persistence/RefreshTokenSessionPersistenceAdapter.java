package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.infrastructure.persistence.entity.RefreshTokenSessionJpaEntity;
import com.promsearch.auth.application.port.out.persistence.refresh.LoadRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.persistence.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.domain.RefreshTokenSession;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenSessionPersistenceAdapter
        implements LoadRefreshTokenSessionPort, SaveRefreshTokenSessionPort {

    private final RefreshTokenSessionRepository repository;

    @Override
    public RefreshTokenSession save(RefreshTokenSession session) {
        if (session.getId() == null) {
            return repository.save(RefreshTokenSessionJpaEntity.from(session)).toDomain();
        }
        RefreshTokenSessionJpaEntity entity = repository.findById(session.getId()).orElseThrow();
        entity.updateFrom(session);
        return entity.toDomain();
    }

    @Override
    public Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshTokenSessionJpaEntity::toDomain);
    }

    @Override
    public void revokeFamily(String familyId, Instant revokedAt) {
        repository.revokeFamily(familyId, revokedAt);
    }
}
