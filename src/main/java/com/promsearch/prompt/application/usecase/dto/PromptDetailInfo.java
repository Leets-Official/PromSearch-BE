package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.TagType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromptDetailInfo(
        Long promptId,
        String title,
        Author author,
        PromptOutputType outputType,
        PromptContentType contentType,
        Long pricePoint,
        String promptBody,
        String description,
        Access access,
        ViewerInteraction viewerInteraction,
        List<Image> images,
        List<Tag> tags,
        List<String> customAiModels,
        Statistics statistics,
        Instant createdAt,
        Instant updatedAt
) {
    public record Author(Long userId, String nickname, String profileImageUrl) {
    }

    public record Access(boolean locked, AccessReason reason) {
    }

    public enum AccessReason {
        ANONYMOUS,
        PREMIUM,
        FREE,
        AUTHOR,
        UNLOCKED
    }

    public record ViewerInteraction(boolean liked, boolean bookmarked) {
    }

    public record Image(UUID imageId, String imageUrl, int sortOrder, boolean thumbnail) {
    }

    public record Tag(Long tagId, TagType tagType, String name) {
    }

    public record Statistics(long viewCount, long copyCount, long likeCount, long commentCount) {
    }
}
