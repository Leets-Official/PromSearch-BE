package com.promsearch.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "페이지네이션 없이 전체 댓글을 반환하는 목록 응답")
public record CommentListResponse(
        @Schema(description = "작성 시간 내림차순으로 정렬된 최상위 댓글 목록")
        List<CommentResponse> comments
) {
}
