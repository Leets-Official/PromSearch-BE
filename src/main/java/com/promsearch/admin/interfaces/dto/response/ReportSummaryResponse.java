package com.promsearch.admin.interfaces.dto.response;

import com.promsearch.moderation.application.usecase.dto.ReportInfo;
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
        Instant createdAt,
        @Schema(description = "신고 대상 요약 정보")
        TargetSummaryResponse targetSummary
) {

    public static ReportSummaryResponse from(ReportInfo info) {
        return new ReportSummaryResponse(
                info.reportId(),
                info.targetType(),
                info.targetId(),
                info.reason(),
                info.description(),
                info.status(),
                info.reporterId(),
                info.createdAt(),
                TargetSummaryResponse.from(info.targetSummary())
        );
    }

    @Schema(description = "신고 대상 요약 정보")
    public record TargetSummaryResponse(
            @Schema(description = "대상 내용(게시글 제목 또는 댓글 본문)", example = "금융 대시보드 UI 프롬프트")
            String content,
            @Schema(description = "대상 작성자 식별자", example = "12")
            Long authorId,
            @Schema(description = "대상 작성자 닉네임", example = "prompt-maker")
            String authorNickname,
            @Schema(description = "이미 삭제되었거나 존재하지 않는 대상인지 여부", example = "false")
            boolean deleted
    ) {

        public static TargetSummaryResponse from(ReportInfo.TargetSummaryInfo info) {
            return new TargetSummaryResponse(
                    info.content(),
                    info.authorId(),
                    info.authorNickname(),
                    info.deleted()
            );
        }
    }
}
