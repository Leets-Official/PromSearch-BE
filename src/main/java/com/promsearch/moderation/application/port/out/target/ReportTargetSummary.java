package com.promsearch.moderation.application.port.out.target;

public record ReportTargetSummary(
        Long targetId,
        String content,
        Long authorId,
        String authorNickname,
        boolean deleted
) {
}
