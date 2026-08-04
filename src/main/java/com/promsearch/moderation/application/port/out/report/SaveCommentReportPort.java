package com.promsearch.moderation.application.port.out.report;

import com.promsearch.moderation.domain.CommentReport;

public interface SaveCommentReportPort {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    void save(CommentReport report);
}
