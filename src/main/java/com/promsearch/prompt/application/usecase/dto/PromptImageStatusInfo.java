package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import java.util.UUID;

public record PromptImageStatusInfo(
        UUID imageId,
        PromptImageStatus status,
        String failureCode,
        String imageUrl
) {

    public static PromptImageStatusInfo from(PromptImage image, String imageUrl) {
        return new PromptImageStatusInfo(
                image.getPromptImageId().id(),
                image.getStatus(),
                image.getFailureCode(),
                imageUrl
        );
    }
}
