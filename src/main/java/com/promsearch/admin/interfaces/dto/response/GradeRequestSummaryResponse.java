package com.promsearch.admin.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Origin 심사 대기 항목 목록/처리 응답")
public record GradeRequestSummaryResponse(
        @Schema(description = "심사 대기 항목 식별자", example = "1")
        Long gradeRequestId,
        @Schema(description = "신청자 식별자", example = "5")
        Long userId,
        @Schema(description = "신청자 닉네임", example = "hanharam")
        String username,
        @Schema(description = "신청 시점의 등급", example = "PRIME")
        UserGrade currentGrade,
        @Schema(description = "신청 등급", example = "ORIGIN")
        UserGrade requestedGrade,
        @Schema(description = "신청 처리 상태", example = "PENDING")
        GradeRequestStatus status,
        @Schema(description = "게시글 수", example = "12")
        long postCount,
        @Schema(description = "누적 추천(좋아요) 수", example = "84")
        long cumulativeLikeCount,
        @Schema(description = "신청 시각(Prime 등급업 시각)", example = "2026-07-23T12:00:00Z")
        Instant requestedAt,
        @Schema(description = "처리 시각", example = "2026-07-24T09:00:00Z")
        Instant processedAt
) {

    public static GradeRequestSummaryResponse from(GradeRequestSummaryInfo info) {
        return new GradeRequestSummaryResponse(
                info.gradeRequestId(),
                info.userId(),
                info.username(),
                info.currentGrade(),
                info.requestedGrade(),
                info.status(),
                info.postCount(),
                info.cumulativeLikeCount(),
                info.requestedAt(),
                info.processedAt()
        );
    }
}
