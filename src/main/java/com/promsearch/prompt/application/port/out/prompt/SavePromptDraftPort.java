package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import java.util.List;

public interface SavePromptDraftPort {

    PromptDraftInfo saveOrReplaceDraft(Prompt draft, List<Tag> tags);

    void deleteDraft(Long userId);
}
