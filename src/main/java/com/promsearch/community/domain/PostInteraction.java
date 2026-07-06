package com.promsearch.community.domain;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostInteraction {

    private final PostInteractionId postInteractionId;
    private final Long userId;
    private final Long postId;
    private final InteractionType interactionType;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostInteraction(
            PostInteractionId postInteractionId,
            Long userId,
            Long postId,
            InteractionType interactionType,
            Instant createdAt
    ) {
        this.postInteractionId = postInteractionId;
        this.userId = userId;
        this.postId = postId;
        this.interactionType = interactionType;
        this.createdAt = createdAt;
    }

    public static PostInteraction create(Long userId, Long postId, InteractionType interactionType) {
        validateRequired(userId, postId, interactionType);

        return PostInteraction.builder()
                .userId(userId)
                .postId(postId)
                .interactionType(interactionType)
                .createdAt(Instant.now())
                .build();
    }

    public static PostInteraction reconstruct(
            PostInteractionId postInteractionId,
            Long userId,
            Long postId,
            InteractionType interactionType,
            Instant createdAt
    ) {
        validateRequired(userId, postId, interactionType);

        return PostInteraction.builder()
                .postInteractionId(postInteractionId)
                .userId(userId)
                .postId(postId)
                .interactionType(interactionType)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(Long userId, Long postId, InteractionType interactionType) {
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_USER_ID);
        }
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_POST_ID);
        }
        if (interactionType == null) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_TYPE);
        }
    }

    public record PostInteractionId(Long id) {
        public PostInteractionId {
            if (id == null || id <= 0) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_ID);
            }
        }
    }
}
