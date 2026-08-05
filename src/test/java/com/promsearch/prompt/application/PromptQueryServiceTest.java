package com.promsearch.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.application.port.out.prompt.HomePromptReader;
import com.promsearch.prompt.application.service.query.PromptQueryService;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptQueryServiceTest {

    private FakeHomePromptReader homePromptReader;
    private PromptQueryService promptQueryService;

    @BeforeEach
    void setUp() {
        homePromptReader = new FakeHomePromptReader();
        promptQueryService = new PromptQueryService(homePromptReader, null);
    }

    @Test
    void listPromptsDelegatesFiltersToReader() {
        HomePromptListInfo result = promptQueryService.listPrompts(
                HomePromptListQuery.filtered(
                        1L,
                        2L,
                        List.of(3L, 4L, 3L),
                        5L,
                        PromptOutputType.IMAGE,
                        "  썸네일  ",
                        HomePromptSort.POPULAR,
                        0,
                        12
                )
        );

        assertThat(homePromptReader.lastListQuery.viewerUserId()).isEqualTo(1L);
        assertThat(homePromptReader.lastListQuery.jobTagId()).isEqualTo(2L);
        assertThat(homePromptReader.lastListQuery.taskTagIds()).containsExactly(3L, 4L);
        assertThat(homePromptReader.lastListQuery.aiModelTagId()).isEqualTo(5L);
        assertThat(homePromptReader.lastListQuery.outputType()).isEqualTo(PromptOutputType.IMAGE);
        assertThat(homePromptReader.lastListQuery.keyword()).isEqualTo("썸네일");
        assertThat(homePromptReader.lastListQuery.sort()).isEqualTo(HomePromptSort.POPULAR);
        assertThat(result.prompts()).isEmpty();
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

    @Test
    void homePromptListQueryRejectsPageThatCouldOverflowOffset() {
        assertThatThrownBy(() -> HomePromptListQuery.popular(null, HomePromptListQuery.MAX_PAGE + 1, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void homePromptListQueryRejectsShortKeywordAfterTrim() {
        assertThatThrownBy(() -> HomePromptListQuery.filtered(
                null,
                null,
                List.of(),
                null,
                null,
                " a ",
                HomePromptSort.LATEST,
                0,
                12
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyword");
    }

    private static class FakeHomePromptReader implements HomePromptReader {

        private HomePromptListQuery lastListQuery;
        private HomePromptListQuery lastPopularQuery;
        private HomePromptListQuery lastJobQuery;

        @Override
        public HomePromptListInfo listPrompts(HomePromptListQuery query) {
            this.lastListQuery = query;
            return emptyResult(query);
        }

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
