package com.promsearch.moderation.application.port.out.postreport;

import com.promsearch.moderation.domain.PostReport;

public interface SavePostReportPort {

    PostReport update(PostReport postReport);
}
