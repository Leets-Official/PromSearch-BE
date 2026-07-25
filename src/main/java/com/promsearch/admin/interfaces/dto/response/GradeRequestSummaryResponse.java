package com.promsearch.admin.interfaces.dto.response;

import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Origin 등급업 신청 목록/처리 응답. 필드 구성은 신청 생성 방식 확정 전까지 변경될 수 있습니다.")
public record GradeRequestSummaryResponse(
        @Schema(description = "등급업 신청 식별자", example = "1")
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
        @Schema(description = "신청 시각", example = "2026-07-23T12:00:00Z")
        Instant requestedAt,
        @Schema(description = "처리 시각", example = "2026-07-24T09:00:00Z")
        Instant processedAt
) {
}
