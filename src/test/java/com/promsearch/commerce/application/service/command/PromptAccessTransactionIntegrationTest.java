package com.promsearch.commerce.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;
import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import com.promsearch.commerce.infrastructure.persistence.PostCopyPersistenceAdapter;
import com.promsearch.commerce.infrastructure.persistence.PostCopyRepository;
import com.promsearch.commerce.infrastructure.persistence.PostUnlockPersistenceAdapter;
import com.promsearch.commerce.infrastructure.persistence.PostUnlockRepository;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.PromptDetailProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.StatisticsProjection;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.service.query.PromptDetailQueryService;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PostStatisticsRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptAccessPersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
        JpaConfig.class,
        PromptAccessCommandService.class,
        PostUnlockPersistenceAdapter.class,
        PostCopyPersistenceAdapter.class,
        PromptAccessPersistenceAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PromptAccessTransactionIntegrationTest {

    @Autowired PromptAccessCommandService promptAccessCommandService;
    @Autowired PostUnlockPersistenceAdapter postUnlockPersistenceAdapter;
    @Autowired PostUnlockRepository postUnlockRepository;
    @Autowired PostCopyRepository postCopyRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostStatisticsRepository postStatisticsRepository;

    @AfterEach
    void cleanUp() {
        postCopyRepository.deleteAllInBatch();
        postUnlockRepository.deleteAllInBatch();
        postStatisticsRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
    }

    @Test
    void unlockedPremiumPromptReturnsFullBodyFromDetailQuery() {
        Long promptId = savePrompt(1L, PromptContentType.PREMIUM, PromptVisibility.PUBLIC);
        promptAccessCommandService.unlock(new UnlockPromptCommand(2L, promptId));

        LoadPromptDetailPort loadPromptDetailPort = mock(LoadPromptDetailPort.class);
        PresignPromptImageDownloadPort imageStorage = mock(PresignPromptImageDownloadPort.class);
        when(loadPromptDetailPort.findPublicById(promptId, 2L))
                .thenReturn(Optional.of(detailProjection(promptId)));
        PromptDetailQueryService detailService = new PromptDetailQueryService(
                loadPromptDetailPort,
                postUnlockPersistenceAdapter,
                imageStorage
        );

        assertThat(detailService.get(promptId, 2L).promptBody()).isEqualTo("full prompt body");
        assertThat(detailService.get(promptId, 2L).access().locked()).isFalse();
    }

    @Test
    void privatePromptCannotBeUnlocked() {
        Long promptId = savePrompt(1L, PromptContentType.PREMIUM, PromptVisibility.PRIVATE);

        assertThatThrownBy(() -> promptAccessCommandService.unlock(
                new UnlockPromptCommand(2L, promptId)))
                .isInstanceOf(CommerceDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommerceErrorCode.PROMPT_NOT_ACCESSIBLE);

        assertThat(postUnlockRepository.count()).isZero();
    }

    @Test
    void concurrentDuplicateCopyIsRecordedAndCountedOnce() throws Exception {
        Long promptId = savePrompt(1L, PromptContentType.FREE, PromptVisibility.PUBLIC);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> copyConcurrently(promptId, ready, start)),
                    executor.submit(() -> copyConcurrently(promptId, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(results)
                    .extracting(result -> result.get(5, TimeUnit.SECONDS))
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(postCopyRepository.count()).isEqualTo(1L);
        assertThat(copyCount(promptId)).isEqualTo(1L);
    }

    @Test
    void concurrentDuplicateUnlockIsRecordedOnce() throws Exception {
        Long promptId = savePrompt(1L, PromptContentType.PREMIUM, PromptVisibility.PUBLIC);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> unlockConcurrently(promptId, ready, start)),
                    executor.submit(() -> unlockConcurrently(promptId, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(results)
                    .extracting(result -> result.get(5, TimeUnit.SECONDS))
                    .containsOnly(true);
        }

        assertThat(postUnlockRepository.count()).isEqualTo(1L);
    }

    private boolean copyConcurrently(
            Long promptId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        return promptAccessCommandService.copy(new CopyPromptCommand(2L, promptId)).newlyCounted();
    }

    private boolean unlockConcurrently(
            Long promptId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        promptAccessCommandService.unlock(new UnlockPromptCommand(2L, promptId));
        return true;
    }

    private Long savePrompt(
            Long authorId,
            PromptContentType contentType,
            PromptVisibility visibility
    ) {
        Prompt prompt = Prompt.createActive(
                authorId,
                "prompt title",
                "full prompt body",
                PromptOutputType.TEXT,
                "description",
                contentType,
                visibility,
                contentType == PromptContentType.FREE ? 0L : 100L
        );
        PostJpaEntity post = PostJpaEntity.from(prompt);
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        return postRepository.saveAndFlush(post).getId();
    }

    private long copyCount(Long promptId) {
        return postStatisticsRepository.findById(promptId)
                .orElseThrow()
                .toDomain()
                .getCopyCount();
    }

    private PromptDetailProjection detailProjection(Long promptId) {
        return new PromptDetailProjection(
                promptId,
                1L,
                "prompt title",
                "author",
                null,
                PromptOutputType.TEXT,
                PromptContentType.PREMIUM,
                100L,
                "full prompt body",
                "description",
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                new StatisticsProjection(0L, 0L, 0L, 0L),
                Instant.now(),
                Instant.now()
        );
    }
}
