package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.CommentReport;
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
                ReportTargetType.POST,
                postReport.getPostId(),
                postReport.getReason(),
                postReport.getDescription(),
                postReport.getStatus(),
                postReport.getReporterId(),
                postReport.getCreatedAt()
        );
    }

    public static ReportInfo from(CommentReport commentReport) {
        return new ReportInfo(
                commentReport.getCommentReportId().id(),
                ReportTargetType.COMMENT,
                commentReport.getCommentId(),
                commentReport.getReason(),
                commentReport.getDescription(),
                commentReport.getStatus(),
                commentReport.getReporterId(),
                commentReport.getCreatedAt()
        );
    }
}
