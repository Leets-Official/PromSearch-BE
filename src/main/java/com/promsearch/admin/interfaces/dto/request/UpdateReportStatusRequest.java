package com.promsearch.admin.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 처리 상태 변경 요청")
public record UpdateReportStatusRequest(
        @Schema(description = "처리할 신고의 대상 타입. reportId가 targetType별로 별도 테이블에 저장되어 함께 필요합니다.", example = "POST")
        @NotNull(message = "targetType must not be null")
        ReportTargetType targetType,
        @Schema(description = "변경할 처리 상태. PENDING으로는 되돌릴 수 없습니다.", example = "RESOLVED")
        @NotNull(message = "status must not be null")
        ReportStatus status
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "status must be RESOLVED or REJECTED")
    public boolean isProcessableStatus() {
        return status == null || status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED;
    }

    public UpdateReportStatusCommand toCommand(Long reportId) {
        return UpdateReportStatusCommand.of(reportId, targetType, status);
    }
}
