package com.promsearch.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.prompt.application.port.out.HomePromptReader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptQueryServiceTest {

    private FakeHomePromptReader homePromptReader;
    private PromptQueryService promptQueryService;

    @BeforeEach
    void setUp() {
        homePromptReader = new FakeHomePromptReader();
        promptQueryService = new PromptQueryService(homePromptReader);
    }

    @Test
    void listPopularPromptsDelegatesViewerAndPagingToReader() {
        HomePromptListInfo result = promptQueryService.listPopularPrompts(
                HomePromptListQuery.popular(1L, 0, 12)
        );

        assertThat(homePromptReader.lastPopularQuery.viewerUserId()).isEqualTo(1L);
        assertThat(homePromptReader.lastPopularQuery.page()).isZero();
        assertThat(homePromptReader.lastPopularQuery.size()).isEqualTo(12);
        assertThat(result.prompts()).isEmpty();
    }

    @Test
    void listJobPromptsDelegatesJobTagToReader() {
        promptQueryService.listJobPrompts(HomePromptListQuery.job(null, 3L, 1, 8));

        assertThat(homePromptReader.lastJobQuery.viewerUserId()).isNull();
        assertThat(homePromptReader.lastJobQuery.jobTagId()).isEqualTo(3L);
        assertThat(homePromptReader.lastJobQuery.page()).isEqualTo(1);
        assertThat(homePromptReader.lastJobQuery.size()).isEqualTo(8);
    }

    private static class FakeHomePromptReader implements HomePromptReader {

        private HomePromptListQuery lastPopularQuery;
        private HomePromptListQuery lastJobQuery;

        @Override
        public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
            this.lastPopularQuery = query;
            return emptyResult(query);
        }

        @Override
        public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
            this.lastJobQuery = query;
            return emptyResult(query);
        }

        private HomePromptListInfo emptyResult(HomePromptListQuery query) {
            return new HomePromptListInfo(List.of(), query.page(), query.size(), 0, false);
        }
    }
}
