package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.PromptDetailProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.StatisticsProjection;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.AccessReason;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptDetailQueryServiceTest {

    private final LoadPromptDetailPort loadPromptDetailPort = mock(LoadPromptDetailPort.class);
    private final CheckPostUnlockPort checkPostUnlockPort = mock(CheckPostUnlockPort.class);
    private final PresignPromptImageDownloadPort imageStorage =
            mock(PresignPromptImageDownloadPort.class);
    private final PromptDetailQueryService service = new PromptDetailQueryService(
            loadPromptDetailPort,
            checkPostUnlockPort,
            imageStorage
    );

    private PromptDetailProjection projection;

    @BeforeEach
    void setUp() {
        projection = projection(
                "12345678901234567890",
                PromptContentType.PREMIUM,
                false
        );
        when(loadPromptDetailPort.findPublicById(eq(10L), nullable(Long.class)))
                .thenAnswer(invocation -> Optional.of(projection));
    }

    @Test
    @DisplayName("비회원에게는 본문을 숨기고 전체 좋아요 수는 공개한다")
    void anonymousDetail() {
        PromptDetailInfo result = service.get(10L, null);

        assertThat(result.promptBody()).isEmpty();
        assertThat(result.access().locked()).isTrue();
        assertThat(result.access().reason()).isEqualTo(AccessReason.ANONYMOUS);
        assertThat(result.viewerInteraction().liked()).isFalse();
        assertThat(result.customAiModels()).containsExactly("GPT 4.1 Mini");
        assertThat(result.statistics().likeCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("미구매 사용자의 좋아요 여부와 프리미엄 미리보기를 반환한다")
    void lockedPremiumDetailWithLike() {
        projection = projection(
                "12345678901234567890",
                PromptContentType.PREMIUM,
                true
        );
        when(checkPostUnlockPort.isUnlocked(2L, 10L)).thenReturn(false);

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("12");
        assertThat(result.access().reason()).isEqualTo(AccessReason.PREMIUM);
        assertThat(result.viewerInteraction().liked()).isTrue();
        assertThat(result.statistics().likeCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("프리미엄 미리보기는 이모지를 깨뜨리지 않고 사용자에게 보이는 문자 단위로 자른다")
    void premiumPreviewDoesNotSplitEmoji() {
        projection = projection("😀123456789", PromptContentType.PREMIUM, false);
        when(checkPostUnlockPort.isUnlocked(2L, 10L)).thenReturn(false);

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("😀");
        assertThat(result.access().reason()).isEqualTo(AccessReason.PREMIUM);
    }

    @Test
    @DisplayName("무료 프롬프트는 전체 본문과 FREE 접근 사유를 반환한다")
    void freePromptDetail() {
        projection = projection(
                "12345678901234567890",
                PromptContentType.FREE,
                false
        );

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("12345678901234567890");
        assertThat(result.access().locked()).isFalse();
        assertThat(result.access().reason()).isEqualTo(AccessReason.FREE);
    }

    @Test
    @DisplayName("작성자는 프리미엄 전체 본문과 AUTHOR 접근 사유를 반환받는다")
    void premiumAuthorDetail() {
        PromptDetailInfo result = service.get(10L, 1L);

        assertThat(result.promptBody()).isEqualTo("12345678901234567890");
        assertThat(result.access().locked()).isFalse();
        assertThat(result.access().reason()).isEqualTo(AccessReason.AUTHOR);
    }

    @Test
    @DisplayName("구매 사용자는 프리미엄 전체 본문과 UNLOCKED 접근 사유를 반환받는다")
    void unlockedPremiumDetail() {
        when(checkPostUnlockPort.isUnlocked(2L, 10L)).thenReturn(true);

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("12345678901234567890");
        assertThat(result.access().locked()).isFalse();
        assertThat(result.access().reason()).isEqualTo(AccessReason.UNLOCKED);
    }

    private PromptDetailProjection projection(
            String promptBody,
            PromptContentType contentType,
            boolean liked
    ) {
        return new PromptDetailProjection(
                10L,
                1L,
                "회의록 정리",
                "작성자",
                null,
                PromptOutputType.TEXT,
                contentType,
                contentType == PromptContentType.FREE ? 0L : 500L,
                promptBody,
                "회의 내용을 정리합니다.",
                liked,
                false,
                List.of(),
                List.of(),
                List.of("GPT 4.1 Mini"),
                new StatisticsProjection(100L, 12L, 7L, 3L),
                Instant.parse("2026-07-28T01:00:00Z"),
                Instant.parse("2026-07-28T02:00:00Z")
        );
    }
}
