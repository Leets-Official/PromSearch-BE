package com.promsearch.prompt.infrastructure.query;

import com.promsearch.commerce.infrastructure.persistence.PostUnlockRepository;
import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.GetPromptDetailUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Access;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.AccessReason;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Author;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Image;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Statistics;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Tag;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.ViewerInteraction;
import com.promsearch.prompt.domain.PostStatistics;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptDetailQueryService implements GetPromptDetailUseCase {

    private static final int PREMIUM_PREVIEW_MAX_LENGTH = 200;

    private final PostRepository postRepository;
    private final PromptImageRepository promptImageRepository;
    private final UserRepository userRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final PostUnlockRepository postUnlockRepository;
    private final PresignPromptImageDownloadPort imageStorage;

    @Override
    public PromptDetailInfo get(Long promptId, Long viewerId) {
        if (promptId == null || promptId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        PostJpaEntity post = postRepository.findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                        promptId, PromptStatus.ACTIVE, PromptVisibility.PUBLIC)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));
        User author = userRepository.findByIdAndStatus(post.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND))
                .toDomain();

        boolean owner = viewerId != null && viewerId.equals(post.getUserId());
        boolean unlocked = owner || (viewerId != null
                && post.getContentType() == PromptContentType.PREMIUM
                && postUnlockRepository.existsByUserIdAndPostId(viewerId, promptId));
        BodyAccess bodyAccess = resolveBody(post, viewerId, owner, unlocked);

        boolean recommended = viewerId != null && postInteractionRepository
                .existsByUserIdAndPostIdAndInteractionType(viewerId, promptId, InteractionType.LIKE);
        boolean bookmarked = viewerId != null && postInteractionRepository
                .existsByUserIdAndPostIdAndInteractionType(viewerId, promptId, InteractionType.BOOKMARK);

        List<Image> images = promptImageRepository
                .findAllByPromptIdAndStatusOrderBySortOrderAsc(promptId, PromptImageStatus.READY)
                .stream()
                .filter(image -> image.getWatermarkedObjectKey() != null
                        && !image.getWatermarkedObjectKey().isBlank())
                .map(this::toImageResponse)
                .toList();
        List<Tag> tags = post.getPostTags().stream()
                .map(postTag -> new Tag(
                        postTag.getTag().getId(),
                        postTag.getTag().getTagType(),
                        postTag.getTag().getTagName()))
                .sorted(Comparator.comparing(Tag::tagType).thenComparing(Tag::tagId))
                .toList();
        PostStatistics statistics = post.getStatistics() == null
                ? PostStatistics.create(promptId)
                : post.getStatistics().toDomain();

        return new PromptDetailInfo(
                post.getId(),
                post.getTitle(),
                new Author(
                        author.getUserId().id(), author.getNickname(), author.getProfileImageUrl()),
                post.getOutputType(),
                post.getContentType(),
                post.getPricePoint(),
                bodyAccess.body(),
                post.getDescription(),
                new Access(bodyAccess.locked(), bodyAccess.reason()),
                new ViewerInteraction(recommended, bookmarked),
                images,
                tags,
                new Statistics(
                        statistics.getViewCount(),
                        statistics.getCopyCount(),
                        statistics.getLikeCount(),
                        statistics.getCommentCount()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private BodyAccess resolveBody(
            PostJpaEntity post,
            Long viewerId,
            boolean owner,
            boolean unlocked
    ) {
        String body = post.getPromptBody() == null ? "" : post.getPromptBody();
        if (viewerId == null) {
            return new BodyAccess("", true, AccessReason.ANONYMOUS);
        }
        if (post.getContentType() == PromptContentType.FREE) {
            return new BodyAccess(body, false, AccessReason.FREE);
        }
        if (owner) {
            return new BodyAccess(body, false, AccessReason.AUTHOR);
        }
        if (unlocked) {
            return new BodyAccess(body, false, AccessReason.UNLOCKED);
        }
        int previewLength = Math.min(PREMIUM_PREVIEW_MAX_LENGTH, body.length() / 10);
        return new BodyAccess(body.substring(0, previewLength), true, AccessReason.PREMIUM);
    }

    private Image toImageResponse(PromptImageJpaEntity image) {
        return new Image(
                image.getId(),
                imageStorage.presignGet(image.getWatermarkedObjectKey()),
                image.getSortOrder(),
                image.getThumbnail()
        );
    }

    private record BodyAccess(String body, boolean locked, AccessReason reason) {
    }
}
