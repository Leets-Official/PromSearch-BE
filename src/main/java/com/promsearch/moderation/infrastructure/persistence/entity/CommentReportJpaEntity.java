package com.promsearch.moderation.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.CommentReport.CommentReportId;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportReason;
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
@Table(name = "comment_reports", uniqueConstraints = @UniqueConstraint(
        name = "uk_comment_reports_user_comment", columnNames = {"reporter_id", "comment_id"}))
public class CommentReportJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private CommentReportJpaEntity(Long reporterId, Long commentId, ReportReason reason, String description) {
        this.reporterId = reporterId;
        this.commentId = commentId;
        this.reason = reason;
        this.description = description;
        this.status = ReportStatus.PENDING;
    }

    public static CommentReportJpaEntity from(CommentReport report) {
        return CommentReportJpaEntity.builder()
                .reporterId(report.getReporterId())
                .commentId(report.getCommentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .build();
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }

    public CommentReport toDomain() {
        return CommentReport.reconstruct(
                new CommentReportId(id), reporterId, commentId, reason, description, status, getCreatedAt());
    }
}
