package com.promsearch.community.interfaces.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.promsearch.community.application.usecase.dto.BookmarkInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookmarkResponse(
        @Schema(description = "요청 사용자 기준 북마크 여부", example = "true")
        boolean bookmarked,

        @Schema(
                description = "북마크 등록 시각. 취소 응답에는 포함되지 않습니다.",
                example = "2026-07-13T14:00:00Z",
                nullable = true
        )
        Instant bookmarkedAt
) {

    public static BookmarkResponse from(BookmarkInfo info) {
        return new BookmarkResponse(info.bookmarked(), info.bookmarkedAt());
    }
}
