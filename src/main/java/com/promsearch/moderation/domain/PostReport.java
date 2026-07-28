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
    private final ReportTargetType targetType;
    private final Long targetId;
    private final ReportReason reason;
    private final String description;
    private final ReportStatus status;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostReport(
            PostReportId postReportId,
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String description,
            ReportStatus status,
            Instant createdAt
    ) {
        this.postReportId = postReportId;
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PostReport create(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String description
    ) {
        validateRequired(reporterId, targetType, targetId, reason);

        return PostReport.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public static PostReport reconstruct(
            PostReportId postReportId,
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String description,
            ReportStatus status,
            Instant createdAt
    ) {
        validateRequired(reporterId, targetType, targetId, reason);
        if (status == null) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_STATUS);
        }

        return PostReport.builder()
                .postReportId(postReportId)
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
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
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .status(newStatus)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason
    ) {
        if (reporterId == null || reporterId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORTER_ID);
        }
        if (targetType == null) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_ID);
        }
        if (targetId == null || targetId <= 0) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_POST_ID);
        }
        if (reason == null || !reason.isAllowedFor(targetType)) {
            throw new ModerationDomainException(ModerationErrorCode.INVALID_REPORT_REASON);
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
