package com.promsearch.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.community.application.usecase.dto.LikeInfo;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;
import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.community.infrastructure.persistence.PostInteractionPersistenceAdapter;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PostStatisticsRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptLikePersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
        JpaConfig.class,
        LikeCommandService.class,
        PostInteractionPersistenceAdapter.class,
        PromptLikePersistenceAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LikeCommandTransactionIntegrationTest {

    @Autowired
    private LikeCommandService likeCommandService;

    @Autowired
    private PostInteractionRepository postInteractionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostStatisticsRepository postStatisticsRepository;

    @AfterEach
    void cleanUp() {
        postInteractionRepository.deleteAllInBatch();
        postStatisticsRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
    }

    @DisplayName("좋아요 등록과 통계 증가는 한 트랜잭션에서 저장된다")
    @Test
    void likeStoresInteractionAndIncreasesCount() {
        Long promptId = savePrompt(PromptVisibility.PUBLIC);

        LikeInfo info = likeCommandService.like(new LikePromptCommand(1L, promptId));

        assertThat(info).isEqualTo(new LikeInfo(promptId, true, 1L));
        assertThat(postInteractionRepository.existsByUserIdAndPostIdAndInteractionType(
                1L,
                promptId,
                InteractionType.LIKE
        )).isTrue();
        assertThat(likeCount(promptId)).isEqualTo(1L);
    }

    @DisplayName("좋아요 취소와 통계 감소는 한 트랜잭션에서 저장된다")
    @Test
    void unlikeDeletesInteractionAndDecreasesCount() {
        Long promptId = savePrompt(PromptVisibility.PUBLIC);
        likeCommandService.like(new LikePromptCommand(1L, promptId));

        LikeInfo info = likeCommandService.unlike(new LikePromptCommand(1L, promptId));

        assertThat(info).isEqualTo(new LikeInfo(promptId, false, 0L));
        assertThat(postInteractionRepository.existsByUserIdAndPostIdAndInteractionType(
                1L,
                promptId,
                InteractionType.LIKE
        )).isFalse();
        assertThat(likeCount(promptId)).isZero();
    }

    @DisplayName("동일 사용자의 중복 좋아요는 한 건만 저장하고 좋아요 수도 한 번만 증가한다")
    @Test
    void duplicateLikeIsRejectedWithoutDoubleCounting() {
        Long promptId = savePrompt(PromptVisibility.PUBLIC);
        likeCommandService.like(new LikePromptCommand(1L, promptId));

        assertThatThrownBy(() -> likeCommandService.like(new LikePromptCommand(1L, promptId)))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.ALREADY_INTERACTED);

        assertThat(postInteractionRepository.count()).isEqualTo(1L);
        assertThat(likeCount(promptId)).isEqualTo(1L);
    }

    @DisplayName("비공개 프롬프트의 좋아요는 상호작용 저장까지 롤백한다")
    @Test
    void privatePromptRollsBackInteraction() {
        Long promptId = savePrompt(PromptVisibility.PRIVATE);

        assertThatThrownBy(() -> likeCommandService.like(new LikePromptCommand(1L, promptId)))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.INTERACTION_TARGET_NOT_FOUND);

        assertThat(postInteractionRepository.count()).isZero();
        assertThat(likeCount(promptId)).isZero();
    }

    @DisplayName("동시에 같은 좋아요를 요청해도 상호작용과 좋아요 수는 한 건만 반영된다")
    @Test
    void concurrentDuplicateLikeIsCountedOnce() throws Exception {
        Long promptId = savePrompt(PromptVisibility.PUBLIC);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<CommunityErrorCode>> futures = List.of(
                    executor.submit(() -> likeConcurrently(promptId, ready, start)),
                    executor.submit(() -> likeConcurrently(promptId, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(futures)
                    .extracting(future -> future.get(5, TimeUnit.SECONDS))
                    .containsExactlyInAnyOrder(null, CommunityErrorCode.ALREADY_INTERACTED);
        }

        assertThat(postInteractionRepository.count()).isEqualTo(1L);
        assertThat(likeCount(promptId)).isEqualTo(1L);
    }

    private CommunityErrorCode likeConcurrently(
            Long promptId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            likeCommandService.like(new LikePromptCommand(1L, promptId));
            return null;
        } catch (CommunityDomainException exception) {
            return (CommunityErrorCode) exception.getBaseCode();
        }
    }

    private Long savePrompt(PromptVisibility visibility) {
        Prompt prompt = Prompt.createActive(
                1L,
                "좋아요 테스트 프롬프트",
                "본문",
                PromptOutputType.TEXT,
                "설명",
                PromptContentType.FREE,
                visibility,
                0L
        );
        PostJpaEntity post = PostJpaEntity.from(prompt);
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        return postRepository.saveAndFlush(post).getId();
    }

    private long likeCount(Long promptId) {
        return postStatisticsRepository.findById(promptId)
                .orElseThrow()
                .getLikeCount();
    }
}
