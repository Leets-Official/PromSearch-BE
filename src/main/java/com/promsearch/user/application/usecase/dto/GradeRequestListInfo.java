package com.promsearch.user.application.usecase.dto;

import java.util.List;

public record GradeRequestListInfo(
        List<GradeRequestSummaryInfo> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
