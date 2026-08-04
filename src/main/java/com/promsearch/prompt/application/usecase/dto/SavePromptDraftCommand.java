package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.util.List;
import java.util.UUID;

public record SavePromptDraftCommand(
        Long userId,
        String title,
        String description,
        PromptOutputType outputType,
        List<Long> jobTagIds,
        List<Long> taskTagIds,
        Long aiModelTagId,
        String customAiModel,
        PromptContentType contentType,
        String promptBody,
        PromptVisibility visibility,
        List<ImageReference> images
) {

    public SavePromptDraftCommand {
        jobTagIds = jobTagIds == null ? List.of() : List.copyOf(jobTagIds);
        taskTagIds = taskTagIds == null ? List.of() : List.copyOf(taskTagIds);
        images = images == null ? List.of() : List.copyOf(images);
    }

    public record ImageReference(UUID imageId, int sortOrder, boolean thumbnail) {
    }
}
