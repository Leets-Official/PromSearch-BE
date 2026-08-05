package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;

public record SearchReportsQuery(ReportTargetType targetType, ReportStatus status, int page, int size) {

    public static SearchReportsQuery of(ReportTargetType targetType, ReportStatus status, int page, int size) {
        return new SearchReportsQuery(targetType, status, page, size);
    }
}
