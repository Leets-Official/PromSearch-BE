package com.promsearch.community.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.TagType;
import java.time.Instant;
import java.util.List;

public record BookmarkListInfo(
        List<BookmarkPromptInfo> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public BookmarkListInfo {
        content = List.copyOf(content);
    }

    public record BookmarkPromptInfo(
            Long promptId,
            String title,
            String thumbnailImage,
            PromptContentType contentType,
            PromptOutputType outputType,
            long pricePoint,
            long viewCount,
            long likeCount,
            AuthorInfo author,
            List<TagInfo> tags,
            Instant bookmarkedAt
    ) {

        public BookmarkPromptInfo {
            tags = List.copyOf(tags);
        }
    }

    public record AuthorInfo(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {
    }

    public record TagInfo(
            Long tagId,
            TagType tagType,
            String name
    ) {
    }
}
