package com.promsearch.prompt.application.usecase;

public interface DeletePromptUseCase {

    void delete(Long promptId, Long userId);
}
