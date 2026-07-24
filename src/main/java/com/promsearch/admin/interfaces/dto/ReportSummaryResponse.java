package com.promsearch.admin.interfaces.dto;

import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "신고 목록/처리 응답")
public record ReportSummaryResponse(
        @Schema(description = "신고 식별자", example = "1")
        Long reportId,
        @Schema(description = "신고 대상 타입", example = "POST")
        ReportTargetType targetType,
        @Schema(description = "신고 대상 식별자(게시물 또는 댓글 ID)", example = "10")
        Long targetId,
        @Schema(description = "신고 사유", example = "SPAM")
        ReportReason reason,
        @Schema(description = "신고 상세 설명", example = "동일 게시물이 반복 도배되고 있습니다.")
        String description,
        @Schema(description = "신고 처리 상태", example = "PENDING")
        ReportStatus status,
        @Schema(description = "신고자 식별자", example = "5")
        Long reporterId,
        @Schema(description = "신고 접수 시각", example = "2026-07-23T12:00:00Z")
        Instant createdAt
) {
}
