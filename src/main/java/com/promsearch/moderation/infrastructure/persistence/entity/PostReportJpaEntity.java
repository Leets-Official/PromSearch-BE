package com.promsearch.moderation.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.PostReport.PostReportId;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "post_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_reports_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}
        )
)
public class PostReportJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private PostReportJpaEntity(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String description
    ) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.description = description;
        this.status = ReportStatus.PENDING;
    }

    public static PostReportJpaEntity create(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String description
    ) {
        return PostReportJpaEntity.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .build();
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }

    public PostReport toDomain() {
        return PostReport.reconstruct(
                new PostReportId(id),
                reporterId,
                targetType,
                targetId,
                reason,
                description,
                status,
                getCreatedAt()
        );
    }
}
