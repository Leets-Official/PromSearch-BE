package com.promsearch.prompt.application.port.out.prompt;

public interface LockPromptDraftPort {

    void lockByUserId(Long userId);
}
