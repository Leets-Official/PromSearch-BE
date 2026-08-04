package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.util.List;
import java.util.UUID;

public record CreatePromptCommand(
        Long userId,
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
        List<ImageReference> images
) {

    public CreatePromptCommand {
        jobTagIds = immutableList(jobTagIds);
        taskTagIds = immutableList(taskTagIds);
        aiModelTagIds = immutableList(aiModelTagIds);
        images = immutableList(images);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record ImageReference(
            UUID imageId,
            int sortOrder,
            boolean thumbnail
    ) {
    }
}
