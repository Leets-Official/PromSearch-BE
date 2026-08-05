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
        @Schema(description = "북마크 프롬프트 카드 목록")
        List<BookmarkPromptResponse> prompts,

        @Schema(description = "페이지 정보")
        BookmarkPageResponse page
) {

    public static BookmarkListResponse from(BookmarkListInfo info) {
        return new BookmarkListResponse(
                info.content().stream()
                        .map(BookmarkPromptResponse::from)
                        .toList(),
                BookmarkPageResponse.from(info)
        );
    }

    @Schema(description = "북마크 목록 페이지 정보")
    public record BookmarkPageResponse(
            @Schema(description = "0부터 시작하는 페이지 번호", example = "0")
            int page,

            @Schema(description = "페이지 크기", example = "12")
            int size,

            @Schema(description = "조건에 맞는 전체 북마크 수", example = "24")
            long totalElements,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean hasNext
    ) {

        private static BookmarkPageResponse from(BookmarkListInfo info) {
            return new BookmarkPageResponse(info.page(), info.size(), info.totalElements(), info.hasNext());
        }
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
