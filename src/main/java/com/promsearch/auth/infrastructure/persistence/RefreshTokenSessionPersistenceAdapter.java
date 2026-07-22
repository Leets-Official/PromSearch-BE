package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.application.port.out.RefreshTokenSessionRepository;
import com.promsearch.auth.domain.RefreshTokenSession;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenSessionPersistenceAdapter implements RefreshTokenSessionRepository {
    private final RefreshTokenSessionJpaRepository repository;

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
