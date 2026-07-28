package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptStatus;

public record ListMyPromptsQuery(Long userId, PromptStatus status, int page, int size) {

    public static ListMyPromptsQuery of(Long userId, PromptStatus status, int page, int size) {
        return new ListMyPromptsQuery(userId, status, page, size);
    }
}
