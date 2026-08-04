package com.promsearch.moderation.interfaces.dto.request;

import com.promsearch.moderation.domain.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @Schema(description = "신고 사유", example = "SPAM")
        @NotNull(message = "reason must not be null")
        ReportReason reason,

        @Schema(description = "신고 상세 설명", example = "광고성 게시글이 반복적으로 노출됩니다.")
        @NotBlank(message = "description must not be blank")
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description
) {
}
