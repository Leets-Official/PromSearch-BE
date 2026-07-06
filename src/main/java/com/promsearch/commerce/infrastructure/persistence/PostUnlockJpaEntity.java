package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.common.BaseEntity;
import com.promsearch.commerce.domain.PostUnlock;
import com.promsearch.commerce.domain.PostUnlock.PostUnlockId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "post_unlocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_unlocks_user_post", columnNames = {"user_id", "post_id"})
)
public class PostUnlockJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_unlock_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Column(name = "paid_point", nullable = false)
    private Long paidPoint;

    @Column(name = "creator_reward_point", nullable = false)
    private Long creatorRewardPoint;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostUnlockJpaEntity(Long postId, Long userId, Long creatorUserId, Long paidPoint, Long creatorRewardPoint) {
        this.postId = postId;
        this.userId = userId;
        this.creatorUserId = creatorUserId;
        this.paidPoint = paidPoint;
        this.creatorRewardPoint = creatorRewardPoint;
    }

    public static PostUnlockJpaEntity create(Long postId, Long userId, Long creatorUserId,
                                             Long paidPoint, Long creatorRewardPoint) {
        return PostUnlockJpaEntity.builder()
                .postId(postId)
                .userId(userId)
                .creatorUserId(creatorUserId)
                .paidPoint(paidPoint)
                .creatorRewardPoint(creatorRewardPoint)
                .build();
    }

    public PostUnlock toDomain() {
        return PostUnlock.reconstruct(
                new PostUnlockId(id),
                postId,
                userId,
                creatorUserId,
                paidPoint,
                creatorRewardPoint,
                unlockedAt
        );
    }

    @PrePersist
    private void setUnlockedAt() {
        this.unlockedAt = Instant.now();
    }
}
