package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromptEditInfo(
        Long promptId,
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
        List<ImageInfo> images,
        PromptStatus status,
        Long pricePoint,
        Instant updatedAt
) {
    public PromptEditInfo {
        jobTagIds = jobTagIds == null ? List.of() : List.copyOf(jobTagIds);
        taskTagIds = taskTagIds == null ? List.of() : List.copyOf(taskTagIds);
        aiModelTagIds = aiModelTagIds == null ? List.of() : List.copyOf(aiModelTagIds);
        images = images == null ? List.of() : List.copyOf(images);
    }

    public record ImageInfo(UUID imageId, String imageUrl, int sortOrder, boolean thumbnail) {
    }
}
