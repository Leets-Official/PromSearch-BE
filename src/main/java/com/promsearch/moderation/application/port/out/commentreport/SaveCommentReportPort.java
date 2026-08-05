package com.promsearch.moderation.application.port.out.commentreport;

import com.promsearch.moderation.domain.CommentReport;

public interface SaveCommentReportPort {

    CommentReport update(CommentReport commentReport);
}
