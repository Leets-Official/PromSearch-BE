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
public class PostReport {

    private final PostReportId postReportId;
    private final Long reporterId;
    private final Long postId;
    private final ReportReason reason;
    private final String description;
    private final ReportStatus status;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostReport(
            PostReportId postReportId,
            Long reporterId,
            Long postId,
            ReportReason reason,
            String description,
            ReportStatus status,
            Instant createdAt
    ) {
        this.postReportId = postReportId;
        this.reporterId = reporterId;
        this.postId = postId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PostReport create(Long reporterId, Long postId, ReportReason reason, String description) {
        validateRequired(reporterId, postId, reason, description);

        return PostReport.builder()
                .reporterId(reporterId)
                .postId(postId)
                .reason(reason)
                .description(description.trim())
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public static PostReport reconstruct(
            PostReportId id,
            Long reporterId,
            Long postId,
            ReportReason reason,
            String description,
            ReportStatus status,
            Instant createdAt
    ) {
        validateRequired(reporterId, postId, reason, description);
        if (status == null) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_STATUS);
        }
        return PostReport.builder()
                .postReportId(id)
                .reporterId(reporterId)
                .postId(postId)
                .reason(reason)
                .description(description)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public PostReport updateStatus(ReportStatus newStatus) {
        if (newStatus != ReportStatus.RESOLVED && newStatus != ReportStatus.REJECTED) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_STATUS);
        }

        return PostReport.builder()
                .postReportId(postReportId)
                .reporterId(reporterId)
                .postId(postId)
                .reason(reason)
                .description(description)
                .status(newStatus)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(
            Long reporterId,
            Long postId,
            ReportReason reason,
            String description
    ) {
        if (reporterId == null || reporterId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORTER_ID);
        }
        if (postId == null || postId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_POST_ID);
        }
        if (reason == null || !reason.isAllowedFor(ReportTargetType.POST)) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_REASON);
        }
        if (description == null || description.isBlank()) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_DESCRIPTION);
        }
    }

    public record PostReportId(Long id) {
        public PostReportId {
            if (id == null || id <= 0) {
                throw new ModerationDomainException(ModerationErrorCode.INVALID_ID);
            }
        }
    }
}
