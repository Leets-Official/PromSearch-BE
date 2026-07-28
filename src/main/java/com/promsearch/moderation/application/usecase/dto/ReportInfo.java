package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import java.time.Instant;

public record ReportInfo(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String description,
        ReportStatus status,
        Long reporterId,
        Instant createdAt
) {

    public static ReportInfo from(PostReport postReport) {
        return new ReportInfo(
                postReport.getPostReportId().id(),
                postReport.getTargetType(),
                postReport.getTargetId(),
                postReport.getReason(),
                postReport.getDescription(),
                postReport.getStatus(),
                postReport.getReporterId(),
                postReport.getCreatedAt()
        );
    }
}
