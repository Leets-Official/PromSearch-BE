package com.promsearch.commerce.domain;

import com.promsearch.commerce.domain.enums.PointReferenceType;
import com.promsearch.commerce.domain.enums.PointTransactionType;
import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PointHistory {

    private final PointHistoryId pointHistoryId;
    private final Long userId;
    private final PointTransactionType pointTransactionType;
    private final Long amount;
    private final Long balanceAfter;
    private final PointReferenceType referenceType;
    private final Long referenceId;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PointHistory(
            PointHistoryId pointHistoryId,
            Long userId,
            PointTransactionType pointTransactionType,
            Long amount,
            Long balanceAfter,
            PointReferenceType referenceType,
            Long referenceId,
            Instant createdAt
    ) {
        this.pointHistoryId = pointHistoryId;
        this.userId = userId;
        this.pointTransactionType = pointTransactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public static PointHistory create(
            Long userId,
            PointTransactionType pointTransactionType,
            Long amount,
            Long balanceAfter,
            PointReferenceType referenceType,
            Long referenceId
    ) {
        validateRequired(userId, pointTransactionType, amount, balanceAfter, referenceType, referenceId);

        return PointHistory.builder()
                .userId(userId)
                .pointTransactionType(pointTransactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdAt(Instant.now())
                .build();
    }

    public static PointHistory reconstruct(
            PointHistoryId pointHistoryId,
            Long userId,
            PointTransactionType pointTransactionType,
            Long amount,
            Long balanceAfter,
            PointReferenceType referenceType,
            Long referenceId,
            Instant createdAt
    ) {
        validateRequired(userId, pointTransactionType, amount, balanceAfter, referenceType, referenceId);

        return PointHistory.builder()
                .pointHistoryId(pointHistoryId)
                .userId(userId)
                .pointTransactionType(pointTransactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(
            Long userId,
            PointTransactionType pointTransactionType,
            Long amount,
            Long balanceAfter,
            PointReferenceType referenceType,
            Long referenceId
    ) {
        if (userId == null || userId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_USER_ID);
        }
        if (pointTransactionType == null) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_TRANSACTION_TYPE);
        }
        if (amount == null || amount == 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_AMOUNT);
        }
        if (balanceAfter == null || balanceAfter < 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_AMOUNT);
        }
        if ((referenceType == null) != (referenceId == null)) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_REFERENCE);
        }
        if (referenceId != null && referenceId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POINT_REFERENCE);
        }
    }

    public record PointHistoryId(Long id) {
        public PointHistoryId {
            if (id == null || id <= 0) {
                throw new CommerceDomainException(CommerceErrorCode.INVALID_ID);
            }
        }
    }
}
