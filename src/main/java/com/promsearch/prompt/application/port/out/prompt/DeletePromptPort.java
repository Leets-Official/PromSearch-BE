package com.promsearch.prompt.application.port.out.prompt;

public interface DeletePromptPort {

    void delete(Long promptId, Long userId);
}
