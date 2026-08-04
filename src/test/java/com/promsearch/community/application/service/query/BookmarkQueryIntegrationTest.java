package com.promsearch.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.promsearch.community.application.service.command.BookmarkCommandService;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;
import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.community.infrastructure.persistence.PostInteractionPersistenceAdapter;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.community.infrastructure.persistence.entity.PostInteractionJpaEntity;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptInteractionTargetPersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.TagRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostTagJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import com.promsearch.prompt.infrastructure.query.BookmarkListQueryAdapter;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.time.Instant;
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
        BookmarkCommandService.class,
        BookmarkQueryService.class,
        BookmarkListQueryAdapter.class,
        PostInteractionPersistenceAdapter.class,
        PromptInteractionTargetPersistenceAdapter.class
})
class BookmarkQueryIntegrationTest {

    @Autowired
    private BookmarkCommandService bookmarkCommandService;

    @Autowired
    private BookmarkQueryService bookmarkQueryService;

    @Autowired
    private PostInteractionRepository postInteractionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PresignPromptImageDownloadPort presignPromptImageDownloadPort;

    @Autowired
    private PromptImageRepository promptImageRepository;

    @DisplayName("태스크·AI 모델·결과물 필터를 조합하고 비공개 프롬프트를 제외한다")
    @Test
    void listBookmarksWithCombinedFilters() {
        Long userId = saveUser("viewer");
        Long authorId = saveUser("author");
        TagJpaEntity task = saveTag(TagType.TASK, "요약");
        TagJpaEntity aiModel = saveTag(TagType.AI_MODEL, "ChatGPT");
        TagJpaEntity anotherModel = saveTag(TagType.AI_MODEL, "Claude");

        Long matchingPromptId = savePrompt(
                authorId,
                "일치 프롬프트",
                PromptOutputType.TEXT,
                PromptVisibility.PUBLIC,
                List.of(task, aiModel)
        );
        Long wrongModelPromptId = savePrompt(
                authorId,
                "다른 모델",
                PromptOutputType.TEXT,
                PromptVisibility.PUBLIC,
                List.of(task, anotherModel)
        );
        Long privatePromptId = savePrompt(
                authorId,
                "비공개 프롬프트",
                PromptOutputType.TEXT,
                PromptVisibility.PRIVATE,
                List.of(task, aiModel)
        );
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, matchingPromptId));
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, wrongModelPromptId));
        postInteractionRepository.saveAndFlush(PostInteractionJpaEntity.create(
                userId,
                privatePromptId,
                InteractionType.BOOKMARK
        ));
        saveReadyThumbnail(authorId, matchingPromptId, "watermarked/matching.jpg");
        when(presignPromptImageDownloadPort.presignGet("watermarked/matching.jpg"))
                .thenReturn("https://signed.example/matching");

        BookmarkListInfo result = bookmarkQueryService.list(new BookmarkListQuery(
                userId,
                task.getId(),
                aiModel.getId(),
                PromptOutputType.TEXT,
                0,
                6
        ));

        assertThat(result.content()).singleElement()
                .satisfies(prompt -> {
                    assertThat(prompt.promptId()).isEqualTo(matchingPromptId);
                    assertThat(prompt.title()).isEqualTo("일치 프롬프트");
                    assertThat(prompt.thumbnailImage()).isEqualTo("https://signed.example/matching");
                    assertThat(prompt.thumbnailImage()).doesNotContain("watermarked/matching.jpg");
                    assertThat(prompt.author().userId()).isEqualTo(authorId);
                    assertThat(prompt.tags())
                            .extracting(BookmarkListInfo.TagInfo::name)
                            .containsExactlyInAnyOrder("요약", "ChatGPT");
                    assertThat(prompt.viewCount()).isZero();
                    assertThat(prompt.likeCount()).isZero();
                });
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("내 북마크는 최신 등록순으로 안정 정렬하고 0 기반 페이지 정보를 반환한다")
    @Test
    void listBookmarksNewestFirstWithPagination() {
        Long userId = saveUser("viewer-page");
        Long authorId = saveUser("author-page");
        Long firstPromptId = savePrompt(
                authorId,
                "첫 번째",
                PromptOutputType.TEXT,
                PromptVisibility.PUBLIC,
                List.of()
        );
        Long secondPromptId = savePrompt(
                authorId,
                "두 번째",
                PromptOutputType.IMAGE,
                PromptVisibility.PUBLIC,
                List.of()
        );
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, firstPromptId));
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, secondPromptId));
        BookmarkListInfo firstPage = bookmarkQueryService.list(new BookmarkListQuery(
                userId,
                null,
                null,
                null,
                0,
                1
        ));

        assertThat(firstPage.content()).extracting(BookmarkListInfo.BookmarkPromptInfo::promptId)
                .containsExactly(secondPromptId);
        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @DisplayName("북마크 취소는 반복 호출해도 성공하고 저장된 기록을 남기지 않는다")
    @Test
    void unbookmarkIsIdempotent() {
        Long userId = saveUser("viewer-delete");
        Long authorId = saveUser("author-delete");
        Long promptId = savePrompt(
                authorId,
                "취소 대상",
                PromptOutputType.TEXT,
                PromptVisibility.PUBLIC,
                List.of()
        );
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, promptId));

        bookmarkCommandService.unbookmark(new BookmarkPromptCommand(userId, promptId));
        bookmarkCommandService.unbookmark(new BookmarkPromptCommand(userId, promptId));

        assertThat(postInteractionRepository.existsByUserIdAndPostIdAndInteractionType(
                userId,
                promptId,
                InteractionType.BOOKMARK
        )).isFalse();
    }

    @DisplayName("비공개 프롬프트에는 새 북마크를 만들 수 없다")
    @Test
    void privatePromptCannotBeBookmarked() {
        Long userId = saveUser("viewer-private");
        Long authorId = saveUser("author-private");
        Long promptId = savePrompt(
                authorId,
                "비공개",
                PromptOutputType.TEXT,
                PromptVisibility.PRIVATE,
                List.of()
        );

        assertThatThrownBy(() -> bookmarkCommandService.bookmark(
                new BookmarkPromptCommand(userId, promptId)
        ))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.INTERACTION_TARGET_NOT_FOUND);
        assertThat(postInteractionRepository.count()).isZero();
    }

    @DisplayName("동일 프롬프트의 중복 북마크는 한 건만 저장한다")
    @Test
    void duplicateBookmarkIsRejected() {
        Long userId = saveUser("viewer-duplicate");
        Long authorId = saveUser("author-duplicate");
        Long promptId = savePrompt(
                authorId,
                "중복 대상",
                PromptOutputType.TEXT,
                PromptVisibility.PUBLIC,
                List.of()
        );
        bookmarkCommandService.bookmark(new BookmarkPromptCommand(userId, promptId));

        assertThatThrownBy(() -> bookmarkCommandService.bookmark(
                new BookmarkPromptCommand(userId, promptId)
        ))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.ALREADY_INTERACTED);
        entityManager.clear();
        assertThat(postInteractionRepository.count()).isEqualTo(1);
    }

    private Long saveUser(String suffix) {
        UserJpaEntity user = userRepository.saveAndFlush(UserJpaEntity.create(
                suffix + "@example.com",
                "password",
                suffix,
                suffix,
                null,
                null
        ));
        return user.toDomain().getUserId().id();
    }

    private TagJpaEntity saveTag(TagType type, String name) {
        return tagRepository.saveAndFlush(TagJpaEntity.create(
                type,
                name,
                name.toLowerCase(),
                false
        ));
    }

    private Long savePrompt(
            Long authorId,
            String title,
            PromptOutputType outputType,
            PromptVisibility visibility,
            List<TagJpaEntity> tags
    ) {
        PostJpaEntity post = postRepository.saveAndFlush(PostJpaEntity.from(Prompt.createActive(
                authorId,
                title,
                "본문",
                outputType,
                "설명",
                PromptContentType.FREE,
                visibility,
                0L
        )));
        tags.forEach(tag -> post.addPostTag(PostTagJpaEntity.create(post, tag)));
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        postRepository.flush();
        return post.getId();
    }

    private void saveReadyThumbnail(Long uploaderId, Long promptId, String watermarkedObjectKey) {
        PromptImage image = PromptImage.prepareUpload(
                UUID.randomUUID(),
                uploaderId,
                "original/matching.jpg",
                "matching.jpg",
                PromptImageContentType.JPEG,
                1_024L,
                100,
                100
        );
        image.completeUpload("etag", Instant.parse("2026-07-13T13:00:00Z"));
        image.startProcessing();
        image.completeProcessing(watermarkedObjectKey, 1);
        image.attachToPrompt(promptId, uploaderId, 0, true);
        promptImageRepository.saveAndFlush(PromptImageJpaEntity.from(image));
    }
}
