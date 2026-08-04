package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;

public record UpdateReportStatusCommand(Long reportId, ReportTargetType targetType, ReportStatus status) {

    public static UpdateReportStatusCommand of(Long reportId, ReportTargetType targetType, ReportStatus status) {
        return new UpdateReportStatusCommand(reportId, targetType, status);
    }
}
