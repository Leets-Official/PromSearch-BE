package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import java.time.Instant;

public record GradeRequestSummaryInfo(
        Long gradeRequestId,
        Long userId,
        String username,
        UserGrade currentGrade,
        UserGrade requestedGrade,
        GradeRequestStatus status,
        long postCount,
        long cumulativeLikeCount,
        Instant requestedAt,
        Instant processedAt
) {
}
