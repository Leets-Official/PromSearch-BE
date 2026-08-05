package com.promsearch.community.application.port.out.prompt;

public interface UpdatePromptLikeCountPort {

    long increase(Long promptId);

    long decrease(Long promptId);
}
