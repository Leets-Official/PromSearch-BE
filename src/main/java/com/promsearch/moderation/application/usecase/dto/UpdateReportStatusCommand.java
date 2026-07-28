package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportStatus;

public record UpdateReportStatusCommand(Long reportId, ReportStatus status) {

    public static UpdateReportStatusCommand of(Long reportId, ReportStatus status) {
        return new UpdateReportStatusCommand(reportId, status);
    }
}
