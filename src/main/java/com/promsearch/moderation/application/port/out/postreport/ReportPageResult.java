package com.promsearch.moderation.application.port.out.postreport;

import com.promsearch.moderation.domain.PostReport;
import java.util.List;

public record ReportPageResult(List<PostReport> content, long totalElements) {

    public ReportPageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
