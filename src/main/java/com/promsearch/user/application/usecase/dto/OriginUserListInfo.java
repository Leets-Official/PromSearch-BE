package com.promsearch.user.application.usecase.dto;

import java.util.List;

public record OriginUserListInfo(
        List<OriginUserSummaryInfo> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
