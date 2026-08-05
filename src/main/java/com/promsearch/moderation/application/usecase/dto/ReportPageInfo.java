package com.promsearch.moderation.application.usecase.dto;

import java.util.List;

public record ReportPageInfo(List<ReportInfo> content, long totalElements) {

    public ReportPageInfo {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
