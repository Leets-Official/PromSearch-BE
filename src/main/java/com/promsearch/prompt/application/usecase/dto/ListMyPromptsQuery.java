package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;

public record ListMyPromptsQuery(Long userId, PromptStatus status, PromptVisibility visibility, int page, int size) {

    public static ListMyPromptsQuery of(
            Long userId,
            PromptStatus status,
            PromptVisibility visibility,
            int page,
            int size
    ) {
        return new ListMyPromptsQuery(userId, status, visibility, page, size);
    }
}
