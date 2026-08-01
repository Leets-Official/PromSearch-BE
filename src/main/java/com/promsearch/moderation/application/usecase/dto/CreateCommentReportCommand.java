package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportReason;

public record CreateCommentReportCommand(Long reporterId, Long commentId, ReportReason reason, String description) {
}
