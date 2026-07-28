package com.promsearch.moderation.application.port.out.postreport;

import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;

public interface LoadPostReportPort {

    PostReport getById(Long reportId);

    ReportPageResult search(ReportTargetType targetType, ReportStatus status, int page, int size);
}
