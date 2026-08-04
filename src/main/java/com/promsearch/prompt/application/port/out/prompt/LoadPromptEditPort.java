package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPromptEditPort {

    Optional<PromptEditProjection> findById(Long promptId);

    record PromptEditProjection(
            Long promptId,
            Long authorId,
            String title,
            String description,
            PromptOutputType outputType,
            List<Long> jobTagIds,
            List<Long> taskTagIds,
            List<Long> aiModelTagIds,
            String customAiModel,
            PromptContentType contentType,
            String promptBody,
            PromptVisibility visibility,
            List<ImageProjection> images,
            PromptStatus status,
            Long pricePoint,
            Instant updatedAt
    ) {
    }

    record ImageProjection(UUID imageId, String watermarkedObjectKey, int sortOrder, boolean thumbnail) {
    }
}
