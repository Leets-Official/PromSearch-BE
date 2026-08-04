package com.promsearch.moderation.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.PostReport.PostReportId;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
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
                name = "uk_post_reports_user_post",
                columnNames = {"reporter_id", "post_id"}
        )
)
public class PostReportJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private PostReportJpaEntity(
            Long reporterId,
            Long postId,
            ReportReason reason,
            String description
    ) {
        this.reporterId = reporterId;
        this.postId = postId;
        this.reason = reason;
        this.description = description;
        this.status = ReportStatus.PENDING;
    }

    public static PostReportJpaEntity from(PostReport report) {
        return PostReportJpaEntity.builder()
                .reporterId(report.getReporterId())
                .postId(report.getPostId())
                .reason(report.getReason())
                .description(report.getDescription())
                .build();
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }

    public PostReport toDomain() {
        return PostReport.reconstruct(
                new PostReportId(id), reporterId, postId, reason, description, status, getCreatedAt());
    }
}
