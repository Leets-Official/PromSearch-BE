package com.promsearch.moderation.application.usecase.dto;

import com.promsearch.moderation.domain.enums.ReportReason;

public record CreatePostReportCommand(Long reporterId, Long postId, ReportReason reason, String description) {
}
