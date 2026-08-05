package com.promsearch.prompt.application.port.out.prompt;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.TagType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPromptDetailPort {

    Optional<PromptDetailProjection> findPublicById(Long promptId, Long viewerId);

    record PromptDetailProjection(
            Long promptId,
            Long authorId,
            String title,
            String authorNickname,
            String authorProfileImageUrl,
            String authorGradeName,
            PromptOutputType outputType,
            PromptContentType contentType,
            Long pricePoint,
            String promptBody,
            String description,
            boolean recommended,
            boolean bookmarked,
            List<ImageProjection> images,
            List<TagProjection> tags,
            StatisticsProjection statistics,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record ImageProjection(
            UUID imageId,
            String watermarkedObjectKey,
            int sortOrder,
            boolean thumbnail
    ) {
    }

    record TagProjection(Long tagId, TagType tagType, String name) {
    }

    record StatisticsProjection(
            long viewCount,
            long copyCount,
            long recommendCount,
            long commentCount
    ) {
    }
}
