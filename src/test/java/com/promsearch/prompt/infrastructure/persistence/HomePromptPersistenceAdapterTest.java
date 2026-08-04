package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.community.infrastructure.persistence.entity.PostInteractionJpaEntity;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.application.usecase.dto.HomePromptSummaryInfo;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostTagJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({
        JpaConfig.class,
        HomePromptPersistenceAdapter.class
})
class HomePromptPersistenceAdapterTest {

    @Autowired
    private HomePromptPersistenceAdapter homePromptPersistenceAdapter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PromptImageRepository promptImageRepository;

    @Autowired
    private PostInteractionRepository postInteractionRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PresignPromptImageDownloadPort presignPromptImageDownloadPort;

    @MockitoBean
    private ProfileImageDeliveryPort profileImageDeliveryPort;

    @DisplayName("홈 필터 조회 JPQL은 태그, 검색어, 결과물 조건과 공개 범위를 함께 적용한다")
    @Test
    void listPromptsExecutesCombinedFilterQuery() {
        Long authorId = saveUser("author@example.com", "작성자");
        Long viewerId = saveUser("viewer@example.com", "조회자");
        TagJpaEntity jobTag = saveTag(TagType.JOB, "학생", "학생");
        TagJpaEntity taskTag = saveTag(TagType.TASK, "PPT", "ppt");
        TagJpaEntity aiModelTag = saveTag(TagType.AI_MODEL, "ChatGPT", "chatgpt");

        Long publicPromptId = savePrompt(
                authorId,
                "보고서 프롬프트",
                PromptVisibility.PUBLIC,
                List.of(jobTag, taskTag, aiModelTag)
        );
        savePrompt(authorId, "보고서 비공개 프롬프트", PromptVisibility.PRIVATE, List.of(jobTag, taskTag, aiModelTag));
        saveReadyThumbnail(authorId, publicPromptId, "watermarked/%d/thumb.jpg".formatted(publicPromptId));
        saveInteraction(viewerId, publicPromptId, InteractionType.LIKE);
        saveInteraction(viewerId, publicPromptId, InteractionType.BOOKMARK);
        when(presignPromptImageDownloadPort.presignGet(anyString()))
                .thenAnswer(invocation -> "https://cdn.promsearch.test/" + invocation.getArgument(0));

        entityManager.flush();
        entityManager.clear();

        HomePromptListInfo result = homePromptPersistenceAdapter.listPrompts(HomePromptListQuery.filtered(
                viewerId,
                jobTag.getId(),
                List.of(taskTag.getId()),
                List.of(aiModelTag.getId()),
                List.of(PromptOutputType.TEXT),
                "보고서",
                HomePromptSort.POPULAR,
                0,
                12
        ));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        HomePromptSummaryInfo prompt = result.prompts().getFirst();
        assertThat(prompt.promptId()).isEqualTo(publicPromptId);
        assertThat(prompt.thumbnailImageUrl())
                .isEqualTo("https://cdn.promsearch.test/watermarked/%d/thumb.jpg".formatted(publicPromptId));
        assertThat(prompt.viewerInteraction().liked()).isTrue();
        assertThat(prompt.viewerInteraction().bookmarked()).isTrue();
        assertThat(prompt.tags())
                .extracting("tagId")
                .containsExactly(jobTag.getId(), taskTag.getId(), aiModelTag.getId());
    }

    @DisplayName("홈 필터 검색어는 LIKE 와일드카드 문자를 일반 문자로 검색한다")
    @Test
    void listPromptsEscapesKeywordWildcards() {
        Long authorId = saveUser("wildcard-author@example.com", "검색작성자");
        Long percentPromptId = savePrompt(authorId, "100% 실전 프롬프트", PromptVisibility.PUBLIC, List.of());
        savePrompt(authorId, "100점 실전 프롬프트", PromptVisibility.PUBLIC, List.of());
        Long underscorePromptId = savePrompt(authorId, "a_b 프롬프트", PromptVisibility.PUBLIC, List.of());
        savePrompt(authorId, "axb 프롬프트", PromptVisibility.PUBLIC, List.of());
        Long backslashPromptId = savePrompt(authorId, "경로\\파일 프롬프트", PromptVisibility.PUBLIC, List.of());
        savePrompt(authorId, "경로파일 프롬프트", PromptVisibility.PUBLIC, List.of());

        entityManager.flush();
        entityManager.clear();

        assertThat(searchPromptIds("100%")).containsExactly(percentPromptId);
        assertThat(searchPromptIds("a_b")).containsExactly(underscorePromptId);
        assertThat(searchPromptIds("경로\\파일")).containsExactly(backslashPromptId);
    }

    private Long saveUser(String email, String nickname) {
        UserJpaEntity user = userRepository.saveAndFlush(
                UserJpaEntity.create(email, "encoded-password", nickname, nickname, null, null)
        );
        return user.toDomain().getUserId().id();
    }

    private TagJpaEntity saveTag(TagType tagType, String tagName, String normalizedName) {
        return tagRepository.saveAndFlush(TagJpaEntity.create(tagType, tagName, normalizedName, false));
    }

    private List<Long> searchPromptIds(String keyword) {
        return homePromptPersistenceAdapter.listPrompts(HomePromptListQuery.filtered(
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        keyword,
                        HomePromptSort.LATEST,
                        0,
                        12
                ))
                .prompts()
                .stream()
                .map(HomePromptSummaryInfo::promptId)
                .toList();
    }

    private Long savePrompt(
            Long authorId,
            String title,
            PromptVisibility visibility,
            List<TagJpaEntity> tags
    ) {
        Prompt prompt = Prompt.createActive(
                authorId,
                title,
                "프롬프트 본문",
                PromptOutputType.TEXT,
                "프롬프트 설명",
                PromptContentType.FREE,
                visibility,
                0L
        );
        PostJpaEntity post = postRepository.saveAndFlush(PostJpaEntity.from(prompt));
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        tags.forEach(tag -> post.addPostTag(PostTagJpaEntity.create(post, tag)));
        return postRepository.saveAndFlush(post).getId();
    }

    private void saveReadyThumbnail(Long authorId, Long promptId, String watermarkedObjectKey) {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                authorId,
                "originals/%d/%s.jpg".formatted(authorId, imageId),
                "result.jpg",
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
        image.completeUpload("\"etag\"", Instant.parse("2026-07-30T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing(watermarkedObjectKey, 1);
        image.attachToPrompt(promptId, authorId, 0, true);

        promptImageRepository.saveAndFlush(PromptImageJpaEntity.from(image));
    }

    private void saveInteraction(Long viewerId, Long promptId, InteractionType interactionType) {
        postInteractionRepository.saveAndFlush(PostInteractionJpaEntity.create(viewerId, promptId, interactionType));
    }
}
