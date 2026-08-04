package com.promsearch.prompt.infrastructure.query;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.PostInteractionRepository;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.ImageProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.PromptDetailProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.StatisticsProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.TagProjection;
import com.promsearch.prompt.domain.PostStatistics;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostTagJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptDetailQueryAdapter implements LoadPromptDetailPort {

    private final PostRepository postRepository;
    private final PromptImageRepository promptImageRepository;
    private final UserRepository userRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final ProfileImageDeliveryPort profileImageDeliveryPort;

    @Override
    public Optional<PromptDetailProjection> findPublicById(Long promptId, Long viewerId) {
        Optional<PostJpaEntity> postResult =
                postRepository.findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                        promptId, PromptStatus.ACTIVE, PromptVisibility.PUBLIC);
        if (postResult.isEmpty()) {
            return Optional.empty();
        }

        PostJpaEntity post = postResult.get();
        Optional<User> authorResult = userRepository
                .findByIdAndStatus(post.getUserId(), UserStatus.ACTIVE)
                .map(user -> user.toDomain());
        if (authorResult.isEmpty()) {
            return Optional.empty();
        }

        User author = authorResult.get();
        boolean liked = viewerId != null && postInteractionRepository
                .existsByUserIdAndPostIdAndInteractionType(
                        viewerId, promptId, InteractionType.LIKE);
        boolean bookmarked = viewerId != null && postInteractionRepository
                .existsByUserIdAndPostIdAndInteractionType(
                        viewerId, promptId, InteractionType.BOOKMARK);
        List<ImageProjection> images = promptImageRepository
                .findAllByPromptIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(
                        promptId, PromptImageStatus.READY)
                .stream()
                .filter(image -> image.getWatermarkedObjectKey() != null
                        && !image.getWatermarkedObjectKey().isBlank())
                .map(image -> new ImageProjection(
                        image.getId(),
                        image.getWatermarkedObjectKey(),
                        image.getSortOrder(),
                        image.getThumbnail()))
                .toList();
        List<TagProjection> tags = post.getPostTags().stream()
                .filter(postTag -> !Boolean.TRUE.equals(postTag.getTag().getCustom()))
                .map(postTag -> new TagProjection(
                        postTag.getTag().getId(),
                        postTag.getTag().getTagType(),
                        postTag.getTag().getTagName()))
                .sorted(Comparator.comparing(TagProjection::tagType)
                        .thenComparing(TagProjection::tagId))
                .toList();
        List<String> customAiModels = post.getPostTags().stream()
                .map(PostTagJpaEntity::getTag)
                .filter(tag -> tag.getTagType() == TagType.AI_MODEL && Boolean.TRUE.equals(tag.getCustom()))
                .map(TagJpaEntity::getTagName)
                .toList();
        PostStatistics statistics = post.getStatistics() == null
                ? PostStatistics.create(promptId)
                : post.getStatistics().toDomain();

        return Optional.of(new PromptDetailProjection(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                author.getNickname(),
                profileImageDeliveryPort.resolve(
                        author.getProfileImageUrl(),
                        author.getProfileImageObjectKey()
                ),
                post.getOutputType(),
                post.getContentType(),
                post.getPricePoint(),
                post.getPromptBody(),
                post.getDescription(),
                liked,
                bookmarked,
                images,
                tags,
                customAiModels,
                new StatisticsProjection(
                        statistics.getViewCount(),
                        statistics.getCopyCount(),
                        statistics.getLikeCount(),
                        statistics.getCommentCount()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        ));
    }
}
