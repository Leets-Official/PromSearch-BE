package com.promsearch.prompt.infrastructure.persistence;

import java.time.Instant;

public interface MyPromptSummaryProjection {

    Long getPromptId();

    String getTitle();

    Instant getPublishedAt();

    long getViewCount();

    long getRecommendCount();
}
