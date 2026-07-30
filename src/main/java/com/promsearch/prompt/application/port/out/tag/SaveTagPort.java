package com.promsearch.prompt.application.port.out.tag;

import com.promsearch.prompt.domain.Tag;

public interface SaveTagPort {

    Tag create(Tag tag);

    Tag getOrCreateCustomAiModel(Tag tag);
}
