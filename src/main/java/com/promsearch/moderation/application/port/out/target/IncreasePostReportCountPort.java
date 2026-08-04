package com.promsearch.moderation.application.port.out.target;

public interface IncreasePostReportCountPort {

    void increase(Long postId);
}
