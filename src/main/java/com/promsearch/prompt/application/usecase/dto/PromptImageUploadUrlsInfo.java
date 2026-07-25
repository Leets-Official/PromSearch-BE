package com.promsearch.prompt.application.usecase.dto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromptImageUploadUrlsInfo(
        List<UploadTarget> images
) {

    public record UploadTarget(
            UUID imageId,
            URI uploadUrl,
            Instant expiresAt
    ) {
    }
}
