package com.promsearch.moderation.application.port.out.postreport;

import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportStatus;

public interface LoadPostReportPort {

    PostReport getById(Long reportId);

    ReportPageResult search(ReportStatus status, String q, int page, int size);
}
