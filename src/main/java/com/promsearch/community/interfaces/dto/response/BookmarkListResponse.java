package com.promsearch.community.interfaces.dto.response;

import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.AuthorInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.BookmarkPromptInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.TagInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "내 북마크 프롬프트 목록")
public record BookmarkListResponse(
        List<BookmarkPromptResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public static BookmarkListResponse from(BookmarkListInfo info) {
        return new BookmarkListResponse(
                info.content().stream()
                        .map(BookmarkPromptResponse::from)
                        .toList(),
                info.page(),
                info.size(),
                info.totalElements(),
                info.hasNext()
        );
    }

    @Schema(description = "북마크 프롬프트 카드")
    public record BookmarkPromptResponse(
            Long promptId,
            String title,
            String thumbnailImage,
            PromptContentType contentType,
            PromptOutputType outputType,
            long pricePoint,
            long viewCount,
            long likeCount,
            BookmarkAuthorResponse author,
            List<BookmarkTagResponse> tags,
            Instant bookmarkedAt
    ) {

        private static BookmarkPromptResponse from(BookmarkPromptInfo info) {
            return new BookmarkPromptResponse(
                    info.promptId(),
                    info.title(),
                    info.thumbnailImage(),
                    info.contentType(),
                    info.outputType(),
                    info.pricePoint(),
                    info.viewCount(),
                    info.likeCount(),
                    BookmarkAuthorResponse.from(info.author()),
                    info.tags().stream()
                            .map(BookmarkTagResponse::from)
                            .toList(),
                    info.bookmarkedAt()
            );
        }
    }

    @Schema(description = "북마크 카드 작성자")
    public record BookmarkAuthorResponse(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {

        private static BookmarkAuthorResponse from(AuthorInfo info) {
            return new BookmarkAuthorResponse(
                    info.userId(),
                    info.nickname(),
                    info.profileImageUrl()
            );
        }
    }

    @Schema(description = "북마크 카드 태그")
    public record BookmarkTagResponse(
            Long tagId,
            TagType tagType,
            String name
    ) {

        private static BookmarkTagResponse from(TagInfo info) {
            return new BookmarkTagResponse(info.tagId(), info.tagType(), info.name());
        }
    }
}
