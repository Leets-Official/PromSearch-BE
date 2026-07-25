package com.promsearch.auth.infrastructure.persistence.entity;

import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token_sessions")
public class RefreshTokenSessionJpaEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_session_id")
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    private RefreshTokenSessionJpaEntity(Long userId, String tokenHash, String familyId,
                                         Instant expiresAt, Instant revokedAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshTokenSessionJpaEntity from(RefreshTokenSession session) {
        return new RefreshTokenSessionJpaEntity(session.getUserId(), session.getTokenHash(),
                session.getFamilyId(), session.getExpiresAt(), session.getRevokedAt());
    }

    public void updateFrom(RefreshTokenSession session) { this.revokedAt = session.getRevokedAt(); }

    public RefreshTokenSession toDomain() {
        return RefreshTokenSession.reconstruct(id, userId, tokenHash, familyId, expiresAt, revokedAt);
    }
}
