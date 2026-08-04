package com.promsearch.moderation.domain;

import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CommentReport {

    private final CommentReportId commentReportId;
    private final Long reporterId;
    private final Long commentId;
    private final ReportReason reason;
    private final String description;
    private final ReportStatus status;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private CommentReport(CommentReportId commentReportId, Long reporterId, Long commentId, ReportReason reason,
                          String description, ReportStatus status, Instant createdAt) {
        this.commentReportId = commentReportId;
        this.reporterId = reporterId;
        this.commentId = commentId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static CommentReport create(
            Long reporterId,
            Long commentId,
            ReportReason reason,
            String description
    ) {
        validateRequired(reporterId, commentId, reason, description);
        return CommentReport.builder()
                .reporterId(reporterId)
                .commentId(commentId)
                .reason(reason)
                .description(description.trim())
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public static CommentReport reconstruct(CommentReportId id, Long reporterId, Long commentId, ReportReason reason,
                                            String description, ReportStatus status, Instant createdAt) {
        validateRequired(reporterId, commentId, reason, description);
        if (status == null) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_STATUS);
        }
        return CommentReport.builder()
                .commentReportId(id)
                .reporterId(reporterId)
                .commentId(commentId)
                .reason(reason)
                .description(description)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(
            Long reporterId,
            Long commentId,
            ReportReason reason,
            String description
    ) {
        if (reporterId == null || reporterId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORTER_ID);
        }
        if (commentId == null || commentId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_COMMENT_ID);
        }
        if (reason == null || !reason.isAllowedFor(ReportTargetType.COMMENT)) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_REASON);
        }
        if (description == null || description.isBlank()) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_DESCRIPTION);
        }
    }

    public record CommentReportId(Long id) {
        public CommentReportId {
            if (id == null || id <= 0) {
                throw new ModerationDomainException(ModerationErrorCode.INVALID_ID);
            }
        }
    }
}
