package com.promsearch.community.interfaces.dto.response;

import com.promsearch.community.application.usecase.dto.CommentListInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최상위 댓글 커서 페이지 응답")
public record CommentListResponse(
        @Schema(description = "작성 시간 내림차순으로 정렬된 최상위 댓글 목록")
        List<CommentResponse> comments,

        @Schema(description = "다음 페이지 요청에 사용할 마지막 댓글 ID", nullable = true)
        Long nextCursor,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {
    public static CommentListResponse from(CommentListInfo info) {
        return new CommentListResponse(
                info.comments().stream()
                        .map(CommentResponse::from)
                        .toList(),
                info.nextCursor(),
                info.hasNext()
        );
    }
}
