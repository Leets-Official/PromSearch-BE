package com.promsearch.auth.domain;

import java.time.Instant;

public class RefreshTokenSession {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final String familyId;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshTokenSession(
            Long id,
            Long userId,
            String tokenHash,
            String familyId,
            Instant expiresAt,
            Instant revokedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshTokenSession create(Long userId, String tokenHash, String familyId, Instant expiresAt) {
        return new RefreshTokenSession(null, userId, tokenHash, familyId, expiresAt, null);
    }

    public static RefreshTokenSession reconstruct(
            Long id,
            Long userId,
            String tokenHash,
            String familyId,
            Instant expiresAt,
            Instant revokedAt
    ) {
        return new RefreshTokenSession(id, userId, tokenHash, familyId, expiresAt, revokedAt);
    }

    public boolean isAvailableAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public String getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
