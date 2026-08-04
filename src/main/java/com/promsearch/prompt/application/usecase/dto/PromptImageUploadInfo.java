package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import java.time.Instant;
import java.util.UUID;

public record PromptImageUploadInfo(
        UUID imageId,
        PromptImageStatus status,
        Instant uploadedAt
) {

    public static PromptImageUploadInfo from(PromptImage image) {
        return new PromptImageUploadInfo(
                image.getPromptImageId().id(),
                image.getStatus(),
                image.getUploadedAt()
        );
    }
}
