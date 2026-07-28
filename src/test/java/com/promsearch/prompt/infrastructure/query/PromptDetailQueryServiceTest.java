package com.promsearch.prompt.infrastructure.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.promsearch.commerce.infrastructure.persistence.PostUnlockRepository;
import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.AccessReason;
import com.promsearch.prompt.domain.PostStatistics;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptDetailQueryServiceTest {

    private final PostRepository postRepository = mock(PostRepository.class);
    private final PromptImageRepository imageRepository = mock(PromptImageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PostInteractionRepository interactionRepository = mock(PostInteractionRepository.class);
    private final PostUnlockRepository unlockRepository = mock(PostUnlockRepository.class);
    private final PresignPromptImageDownloadPort imageStorage =
            mock(PresignPromptImageDownloadPort.class);
    private final PromptDetailQueryService service = new PromptDetailQueryService(
            postRepository,
            imageRepository,
            userRepository,
            interactionRepository,
            unlockRepository,
            imageStorage
    );

    private PostJpaEntity post;

    @BeforeEach
    void setUp() {
        post = mock(PostJpaEntity.class);
        when(post.getId()).thenReturn(10L);
        when(post.getUserId()).thenReturn(1L);
        when(post.getTitle()).thenReturn("회의록 정리");
        when(post.getPromptBody()).thenReturn("12345678901234567890");
        when(post.getDescription()).thenReturn("회의 내용을 정리합니다.");
        when(post.getOutputType()).thenReturn(PromptOutputType.TEXT);
        when(post.getContentType()).thenReturn(PromptContentType.PREMIUM);
        when(post.getPricePoint()).thenReturn(500L);
        when(post.getPostTags()).thenReturn(List.of());
        when(post.getCreatedAt()).thenReturn(Instant.parse("2026-07-28T01:00:00Z"));
        when(post.getUpdatedAt()).thenReturn(Instant.parse("2026-07-28T02:00:00Z"));

        PostStatisticsJpaEntity statisticsEntity = mock(PostStatisticsJpaEntity.class);
        when(statisticsEntity.toDomain()).thenReturn(
                PostStatistics.reconstruct(10L, 100L, 12L, 7L, 0L, 3L));
        when(post.getStatistics()).thenReturn(statisticsEntity);
        when(postRepository.findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                10L, PromptStatus.ACTIVE, PromptVisibility.PUBLIC))
                .thenReturn(Optional.of(post));
        when(imageRepository.findAllByPromptIdAndStatusOrderBySortOrderAsc(
                10L, com.promsearch.prompt.domain.enums.PromptImageStatus.READY))
                .thenReturn(List.of());

        UserJpaEntity userEntity = mock(UserJpaEntity.class);
        when(userEntity.toDomain()).thenReturn(User.reconstruct(
                new User.UserId(1L),
                "author@example.com",
                "password",
                "작성자",
                "작성자",
                null,
                0L,
                UserRole.USER,
                UserGrade.NORMAL,
                UserStatus.ACTIVE,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z")
        ));
        when(userRepository.findByIdAndStatus(1L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(userEntity));
    }

    @Test
    @DisplayName("비회원에게는 본문을 숨기고 전체 추천 수는 공개한다")
    void anonymousDetail() {
        PromptDetailInfo result = service.get(10L, null);

        assertThat(result.promptBody()).isEmpty();
        assertThat(result.access().locked()).isTrue();
        assertThat(result.access().reason()).isEqualTo(AccessReason.ANONYMOUS);
        assertThat(result.viewerInteraction().recommended()).isFalse();
        assertThat(result.statistics().recommendCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("미언락 사용자의 추천 여부와 프리미엄 미리보기를 반환한다")
    void lockedPremiumDetailWithRecommendation() {
        when(unlockRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(false);
        when(interactionRepository.existsByUserIdAndPostIdAndInteractionType(
                2L, 10L, InteractionType.LIKE)).thenReturn(true);

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("12");
        assertThat(result.access().reason()).isEqualTo(AccessReason.PREMIUM);
        assertThat(result.viewerInteraction().recommended()).isTrue();
        assertThat(result.statistics().recommendCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("무료 프롬프트는 전체 본문과 FREE 접근 사유를 반환한다")
    void freePromptDetail() {
        when(post.getContentType()).thenReturn(PromptContentType.FREE);

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
    @DisplayName("언락 사용자는 프리미엄 전체 본문과 UNLOCKED 접근 사유를 반환받는다")
    void unlockedPremiumDetail() {
        when(unlockRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(true);

        PromptDetailInfo result = service.get(10L, 2L);

        assertThat(result.promptBody()).isEqualTo("12345678901234567890");
        assertThat(result.access().locked()).isFalse();
        assertThat(result.access().reason()).isEqualTo(AccessReason.UNLOCKED);
    }
}
