package com.promsearch.user.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.user.domain.GradeRequest;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "grade_requests")
public class GradeRequestJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_request_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_grade", nullable = false, length = 20)
    private UserGrade currentGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_grade", nullable = false, length = 20)
    private UserGrade requestedGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GradeRequestStatus status;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private GradeRequestJpaEntity(Long userId, UserGrade currentGrade, UserGrade requestedGrade) {
        this.userId = userId;
        this.currentGrade = currentGrade;
        this.requestedGrade = requestedGrade;
        this.status = GradeRequestStatus.PENDING;
    }

    public static GradeRequestJpaEntity createPendingOriginRequest(Long userId) {
        return GradeRequestJpaEntity.builder()
                .userId(userId)
                .currentGrade(UserGrade.PRIME)
                .requestedGrade(UserGrade.ORIGIN)
                .build();
    }

    public void updateFrom(GradeRequest gradeRequest) {
        this.status = gradeRequest.getStatus();
        this.processedAt = gradeRequest.getProcessedAt();
    }

    public GradeRequest toDomain() {
        return GradeRequest.reconstruct(
                id,
                userId,
                currentGrade,
                requestedGrade,
                status,
                getCreatedAt(),
                processedAt
        );
    }

    public Long getId() {
        return id;
    }
}
