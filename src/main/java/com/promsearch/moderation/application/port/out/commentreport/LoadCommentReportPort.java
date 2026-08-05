package com.promsearch.moderation.application.port.out.commentreport;

import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.enums.ReportStatus;

public interface LoadCommentReportPort {

    CommentReport getById(Long reportId);

    CommentReportPageResult search(ReportStatus status, String q, int page, int size);
}
