package com.promsearch.community.application.port.out.comment;

public interface AdjustCommentCountPort {

    void increment(Long postId);

    void decrement(Long postId);
}
