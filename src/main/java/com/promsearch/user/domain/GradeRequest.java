package com.promsearch.user.domain;

import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class GradeRequest {

    private final Long gradeRequestId;
    private final Long userId;
    private final UserGrade currentGrade;
    private final UserGrade requestedGrade;
    private final GradeRequestStatus status;
    private final Instant requestedAt;
    private final Instant processedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private GradeRequest(
            Long gradeRequestId,
            Long userId,
            UserGrade currentGrade,
            UserGrade requestedGrade,
            GradeRequestStatus status,
            Instant requestedAt,
            Instant processedAt
    ) {
        this.gradeRequestId = gradeRequestId;
        this.userId = userId;
        this.currentGrade = currentGrade;
        this.requestedGrade = requestedGrade;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
    }

    public static GradeRequest createPendingOriginRequest(Long userId) {
        if (userId == null || userId <= 0) {
            throw new UserDomainException(UserErrorCode.INVALID_ID);
        }
        return GradeRequest.builder()
                .userId(userId)
                .currentGrade(UserGrade.PRIME)
                .requestedGrade(UserGrade.ORIGIN)
                .status(GradeRequestStatus.PENDING)
                .build();
    }

    public static GradeRequest reconstruct(
            Long gradeRequestId,
            Long userId,
            UserGrade currentGrade,
            UserGrade requestedGrade,
            GradeRequestStatus status,
            Instant requestedAt,
            Instant processedAt
    ) {
        return GradeRequest.builder()
                .gradeRequestId(gradeRequestId)
                .userId(userId)
                .currentGrade(currentGrade)
                .requestedGrade(requestedGrade)
                .status(status)
                .requestedAt(requestedAt)
                .processedAt(processedAt)
                .build();
    }
}
