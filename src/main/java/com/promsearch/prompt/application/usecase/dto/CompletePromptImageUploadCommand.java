package com.promsearch.prompt.application.usecase.dto;

import java.util.UUID;

public record CompletePromptImageUploadCommand(
        Long uploaderId,
        UUID imageId
) {
}
