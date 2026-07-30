package com.promsearch.community.interfaces.dto.response;

import com.promsearch.community.application.usecase.dto.CommentReplyListInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "대댓글 커서 페이지 응답")
public record CommentReplyListResponse(
        @Schema(description = "작성 시간 오름차순으로 정렬된 대댓글 목록")
        List<CommentReplyResponse> replies,

        @Schema(description = "다음 페이지 요청에 사용할 마지막 대댓글 ID", nullable = true)
        Long nextCursor,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {
    public static CommentReplyListResponse from(CommentReplyListInfo info) {
        return new CommentReplyListResponse(
                info.replies().stream()
                        .map(CommentReplyResponse::from)
                        .toList(),
                info.nextCursor(),
                info.hasNext()
        );
    }
}
