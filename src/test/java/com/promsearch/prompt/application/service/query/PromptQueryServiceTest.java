package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.PromptInsightTotals;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;
import com.promsearch.prompt.application.usecase.dto.PromptInsightInfo;
import com.promsearch.prompt.domain.PostStatistics;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Prompt.PromptId;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptQueryServiceTest {

    private final FakeLoadPromptPort loadPromptPort = new FakeLoadPromptPort();
    private final PromptQueryService promptQueryService = new PromptQueryService(loadPromptPort);

    @Test
    void listMyPublishedPromptsMapsSummaryFields() {
        Instant publishedAt = Instant.parse("2026-07-23T12:00:00Z");
        loadPromptPort.result = new PromptPageResult(List.of(testPrompt(1L, "금융 앱 온보딩 UI", publishedAt)), 1L);

        MyPromptPageInfo pageInfo = promptQueryService.listMyPublishedPrompts(
                ListMyPromptsQuery.of(1L, PromptStatus.ACTIVE, 0, 20)
        );

        assertThat(pageInfo.totalElements()).isEqualTo(1L);
        assertThat(pageInfo.content()).hasSize(1);
        assertThat(pageInfo.content().get(0).promptId()).isEqualTo(1L);
        assertThat(pageInfo.content().get(0).title()).isEqualTo("금융 앱 온보딩 UI");
        assertThat(pageInfo.content().get(0).publishedAt()).isEqualTo(publishedAt);
        assertThat(pageInfo.content().get(0).viewCount()).isEqualTo(128L);
        assertThat(pageInfo.content().get(0).recommendCount()).isEqualTo(12L);
    }

    @Test
    void listMyPublishedPromptsRejectsNonActiveStatus() {
        assertThatThrownBy(() -> promptQueryService.listMyPublishedPrompts(
                ListMyPromptsQuery.of(1L, PromptStatus.DRAFT, 0, 20)
        ))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.INVALID_PROMPT_STATUS);
    }

    @Test
    void getMyPromptInsightsReturnsSummedTotals() {
        loadPromptPort.insightTotals = new PromptInsightTotals(1024L, 88L, 42L);

        PromptInsightInfo insightInfo = promptQueryService.getMyPromptInsights(1L);

        assertThat(insightInfo.totalViews()).isEqualTo(1024L);
        assertThat(insightInfo.totalRecommends()).isEqualTo(88L);
        assertThat(insightInfo.totalCopies()).isEqualTo(42L);
    }

    private Prompt testPrompt(Long promptId, String title, Instant publishedAt) {
        Instant now = Instant.now();
        return Prompt.reconstruct(
                new PromptId(promptId),
                1L,
                title,
                "prompt body",
                null,
                PromptOutputType.TEXT,
                "description",
                PromptContentType.FREE,
                PromptStatus.ACTIVE,
                0L,
                null,
                null,
                now,
                now,
                null,
                publishedAt,
                List.of(),
                PostStatistics.reconstruct(promptId, 128L, 42L, 12L, 0L, 0L),
                List.of()
        );
    }

    private static class FakeLoadPromptPort implements LoadPromptPort {

        private PromptPageResult result = new PromptPageResult(List.of(), 0L);
        private PromptInsightTotals insightTotals = new PromptInsightTotals(0L, 0L, 0L);

        @Override
        public PromptPageResult listByUserIdAndStatus(Long userId, PromptStatus status, int page, int size) {
            return result;
        }

        @Override
        public PromptInsightTotals sumInsightsByUserId(Long userId) {
            return insightTotals;
        }
    }
}
