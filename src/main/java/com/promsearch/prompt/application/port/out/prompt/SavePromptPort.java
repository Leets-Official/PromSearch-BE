package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import java.util.List;

public interface SavePromptPort {

    Prompt create(Prompt prompt, List<Tag> tags);
}
