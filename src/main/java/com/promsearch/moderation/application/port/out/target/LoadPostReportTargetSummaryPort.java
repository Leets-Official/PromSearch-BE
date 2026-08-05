package com.promsearch.moderation.application.port.out.target;

import java.util.Collection;
import java.util.List;

public interface LoadPostReportTargetSummaryPort {

    List<ReportTargetSummary> list(Collection<Long> postIds);
}
