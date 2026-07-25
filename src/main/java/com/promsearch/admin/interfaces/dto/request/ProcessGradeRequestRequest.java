package com.promsearch.admin.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Origin 등급업 신청 승인/반려 요청")
public record ProcessGradeRequestRequest(
        @Schema(description = "처리 결과. 반려 시 유저 등급은 변경하지 않습니다.", example = "APPROVED")
        @NotNull(message = "decision must not be null")
        GradeRequestStatus decision
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "decision must be APPROVED or REJECTED")
    public boolean isValidDecision() {
        return decision == null || decision == GradeRequestStatus.APPROVED || decision == GradeRequestStatus.REJECTED;
    }
}
