package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.common.BaseEntity;
import com.promsearch.commerce.domain.PointHistory;
import com.promsearch.commerce.domain.PointHistory.PointHistoryId;
import com.promsearch.commerce.domain.enums.PointReferenceType;
import com.promsearch.commerce.domain.enums.PointTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_histories")
public class PointHistoryJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_transaction_type", nullable = false, length = 40)
    private PointTransactionType pointTransactionType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 50)
    private PointReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Builder(access = AccessLevel.PRIVATE)
    private PointHistoryJpaEntity(Long userId, PointTransactionType pointTransactionType, Long amount,
                                  Long balanceAfter, PointReferenceType referenceType, Long referenceId) {
        this.userId = userId;
        this.pointTransactionType = pointTransactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public static PointHistoryJpaEntity create(Long userId, PointTransactionType pointTransactionType, Long amount,
                                               Long balanceAfter, PointReferenceType referenceType, Long referenceId) {
        return PointHistoryJpaEntity.builder()
                .userId(userId)
                .pointTransactionType(pointTransactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
    }

    public PointHistory toDomain() {
        return PointHistory.reconstruct(
                new PointHistoryId(id),
                userId,
                pointTransactionType,
                amount,
                balanceAfter,
                referenceType,
                referenceId,
                getCreatedAt()
        );
    }
}
