package com.promsearch.commerce.application.port.out.prompt;

public interface LoadPromptAccessTargetPort {

    PromptAccessTarget getByIdForUpdate(Long promptId);

    record PromptAccessTarget(
            Long promptId,
            Long authorId,
            boolean free,
            String promptBody
    ) {
    }
}
