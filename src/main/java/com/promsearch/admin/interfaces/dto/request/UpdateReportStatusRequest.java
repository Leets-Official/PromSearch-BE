package com.promsearch.admin.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 처리 상태 변경 요청")
public record UpdateReportStatusRequest(
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
        return UpdateReportStatusCommand.of(reportId, status);
    }
}
