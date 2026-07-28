package com.promsearch.prompt.infrastructure.persistence;

public interface PromptInsightProjection {

    Long getTotalViews();

    Long getTotalRecommends();

    Long getTotalCopies();
}
