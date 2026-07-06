package com.promsearch.commerce.domain;

import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostUnlock {

    private final PostUnlockId postUnlockId;
    private final Long postId;
    private final Long userId;
    private final Long creatorUserId;
    private final Long paidPoint;
    private final Long creatorRewardPoint;
    private final Instant unlockedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostUnlock(
            PostUnlockId postUnlockId,
            Long postId,
            Long userId,
            Long creatorUserId,
            Long paidPoint,
            Long creatorRewardPoint,
            Instant unlockedAt
    ) {
        this.postUnlockId = postUnlockId;
        this.postId = postId;
        this.userId = userId;
        this.creatorUserId = creatorUserId;
        this.paidPoint = paidPoint;
        this.creatorRewardPoint = creatorRewardPoint;
        this.unlockedAt = unlockedAt;
    }

    public static PostUnlock create(
            Long postId,
            Long userId,
            Long creatorUserId,
            Long paidPoint,
            Long creatorRewardPoint
    ) {
        validateRequired(postId, userId, creatorUserId, paidPoint, creatorRewardPoint);

        return PostUnlock.builder()
                .postId(postId)
                .userId(userId)
                .creatorUserId(creatorUserId)
                .paidPoint(paidPoint)
                .creatorRewardPoint(creatorRewardPoint)
                .unlockedAt(Instant.now())
                .build();
    }

    public static PostUnlock reconstruct(
            PostUnlockId postUnlockId,
            Long postId,
            Long userId,
            Long creatorUserId,
            Long paidPoint,
            Long creatorRewardPoint,
            Instant unlockedAt
    ) {
        validateRequired(postId, userId, creatorUserId, paidPoint, creatorRewardPoint);

        return PostUnlock.builder()
                .postUnlockId(postUnlockId)
                .postId(postId)
                .userId(userId)
                .creatorUserId(creatorUserId)
                .paidPoint(paidPoint)
                .creatorRewardPoint(creatorRewardPoint)
                .unlockedAt(unlockedAt)
                .build();
    }

    private static void validateRequired(
            Long postId,
            Long userId,
            Long creatorUserId,
            Long paidPoint,
            Long creatorRewardPoint
    ) {
        if (postId == null || postId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POST_ID);
        }
        if (userId == null || userId <= 0 || creatorUserId == null || creatorUserId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_USER_ID);
        }
        if (paidPoint == null || paidPoint < 0 || creatorRewardPoint == null || creatorRewardPoint < 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_AMOUNT);
        }
    }

    public record PostUnlockId(Long id) {
        public PostUnlockId {
            if (id == null || id <= 0) {
                throw new CommerceDomainException(CommerceErrorCode.INVALID_ID);
            }
        }
    }
}
