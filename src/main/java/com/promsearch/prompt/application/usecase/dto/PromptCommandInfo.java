package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.time.Instant;

public record PromptCommandInfo(
        Long promptId,
        PromptStatus status,
        PromptVisibility visibility,
        Long pricePoint,
        Instant updatedAt
) {

    public static PromptCommandInfo from(Prompt prompt) {
        return new PromptCommandInfo(
                prompt.getPromptId().id(),
                prompt.getStatus(),
                prompt.getVisibility(),
                prompt.getPricePoint(),
                prompt.getUpdatedAt()
        );
    }
}
