package com.promsearch.auth.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenSessionJpaRepository extends JpaRepository<RefreshTokenSessionJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSessionJpaEntity session where session.tokenHash = :tokenHash")
    Optional<RefreshTokenSessionJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
