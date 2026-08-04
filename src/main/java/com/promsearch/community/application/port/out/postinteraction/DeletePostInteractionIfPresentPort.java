package com.promsearch.community.application.port.out.postinteraction;

import com.promsearch.community.domain.enums.InteractionType;

public interface DeletePostInteractionIfPresentPort {

    void deleteIfPresent(Long userId, Long postId, InteractionType interactionType);
}
