package com.promsearch.prompt.application.usecase.dto;

import java.util.List;
import java.util.UUID;

public record GetPromptImageStatusesQuery(
        Long requesterId,
        List<UUID> imageIds
) {
}
