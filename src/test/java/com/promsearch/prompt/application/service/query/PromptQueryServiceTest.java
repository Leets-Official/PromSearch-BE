package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.MyPromptSummaryRow;
import com.promsearch.prompt.application.port.out.prompt.PromptInsightTotals;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;
import com.promsearch.prompt.application.usecase.dto.PromptInsightInfo;
import com.promsearch.prompt.domain.enums.PromptStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptQueryServiceTest {

    private final FakeLoadPromptPort loadPromptPort = new FakeLoadPromptPort();
    private final PromptQueryService promptQueryService = new PromptQueryService(null, loadPromptPort);

    @Test
    void listMyPromptsMapsSummaryFields() {
        Instant publishedAt = Instant.parse("2026-07-23T12:00:00Z");
        loadPromptPort.result = new PromptPageResult(
                List.of(new MyPromptSummaryRow(1L, "금융 앱 온보딩 UI", publishedAt, 128L, 12L)), 1L);

        MyPromptPageInfo pageInfo = promptQueryService.listMyPrompts(
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
    void listMyPromptsSupportsNonActiveStatus() {
        loadPromptPort.result = new PromptPageResult(List.of(), 0L);

        MyPromptPageInfo pageInfo = promptQueryService.listMyPrompts(
                ListMyPromptsQuery.of(1L, PromptStatus.DRAFT, 0, 20)
        );

        assertThat(loadPromptPort.lastRequestedStatus).isEqualTo(PromptStatus.DRAFT);
        assertThat(pageInfo.totalElements()).isZero();
    }

    @Test
    void getMyPromptInsightsReturnsSummedTotals() {
        loadPromptPort.insightTotals = new PromptInsightTotals(1024L, 88L, 42L);

        PromptInsightInfo insightInfo = promptQueryService.getMyPromptInsights(1L);

        assertThat(insightInfo.totalViews()).isEqualTo(1024L);
        assertThat(insightInfo.totalRecommends()).isEqualTo(88L);
        assertThat(insightInfo.totalCopies()).isEqualTo(42L);
    }

    private static class FakeLoadPromptPort implements LoadPromptPort {

        private PromptPageResult result = new PromptPageResult(List.of(), 0L);
        private PromptInsightTotals insightTotals = new PromptInsightTotals(0L, 0L, 0L);
        private PromptStatus lastRequestedStatus;

        @Override
        public PromptPageResult listByUserIdAndStatus(Long userId, PromptStatus status, int page, int size) {
            lastRequestedStatus = status;
            return result;
        }

        @Override
        public PromptInsightTotals sumInsightsByUserId(Long userId) {
            return insightTotals;
        }
    }
}
