package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;

public record SearchReportsQuery(ReportTargetType targetType, ReportStatus status, String q, int page, int size) {

    public SearchReportsQuery {
        q = normalize(q);
    }

    public static SearchReportsQuery of(ReportTargetType targetType, ReportStatus status, String q, int page, int size) {
        return new SearchReportsQuery(targetType, status, q, page, size);
    }

    private static String normalize(String q) {
        return q == null || q.isBlank() ? null : q.strip();
    }
}
