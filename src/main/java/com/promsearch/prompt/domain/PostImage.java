package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostImage {

    private final PostImageId postImageId;
    private final Long postId;
    private final String imageUrl;
    private final int sortOrder;
    private final boolean thumbnail;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostImage(
            PostImageId postImageId,
            Long postId,
            String imageUrl,
            int sortOrder,
            boolean thumbnail,
            Instant createdAt
    ) {
        this.postImageId = postImageId;
        this.postId = postId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
        this.createdAt = createdAt;
    }

    public static PostImage create(Long postId, String imageUrl, int sortOrder, boolean thumbnail) {
        validateRequired(postId, imageUrl, sortOrder);

        return PostImage.builder()
                .postId(postId)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .createdAt(Instant.now())
                .build();
    }

    public static PostImage reconstruct(
            PostImageId postImageId,
            Long postId,
            String imageUrl,
            int sortOrder,
            boolean thumbnail,
            Instant createdAt
    ) {
        validateRequired(postId, imageUrl, sortOrder);

        return PostImage.builder()
                .postImageId(postImageId)
                .postId(postId)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(Long postId, String imageUrl, int sortOrder) {
        if (postId == null || postId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_URL);
        }
        if (sortOrder < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_ORDER);
        }
    }

    public record PostImageId(Long id) {
        public PostImageId {
            if (id == null || id <= 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
        }
    }
}
