package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.time.Instant;
import java.util.List;

public record HomePromptSummaryInfo(
        Long promptId,
        String title,
        String thumbnailImageUrl,
        PromptOutputType outputType,
        PromptContentType contentType,
        Long pricePoint,
        HomePromptAuthorInfo author,
        HomePromptStatisticsInfo statistics,
        HomePromptViewerInteractionInfo viewerInteraction,
        List<HomePromptTagInfo> tags,
        Instant createdAt
) {
}
