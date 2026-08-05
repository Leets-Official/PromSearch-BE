package com.promsearch.moderation.application.port.out.commentreport;

import com.promsearch.moderation.domain.CommentReport;
import java.util.List;

public record CommentReportPageResult(List<CommentReport> content, long totalElements) {

    public CommentReportPageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
