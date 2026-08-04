package com.promsearch.moderation.application.port.out.report;

import com.promsearch.moderation.domain.PostReport;

public interface SavePostReportPort {

    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);

    void save(PostReport report);
}
