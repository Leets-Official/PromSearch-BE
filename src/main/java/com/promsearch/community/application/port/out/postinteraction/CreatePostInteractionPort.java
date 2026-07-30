package com.promsearch.community.application.port.out.postinteraction;

import com.promsearch.community.domain.PostInteraction;

public interface CreatePostInteractionPort {

    void create(PostInteraction postInteraction);
}
