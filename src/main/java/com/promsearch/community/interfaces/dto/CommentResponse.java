package com.promsearch.community.interfaces.dto;

import com.promsearch.community.domain.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "댓글 및 대댓글 응답")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "101")
        Long commentId,

        @Schema(description = "부모 댓글 ID. 최상위 댓글이면 null입니다.", example = "100", nullable = true)
        Long parentCommentId,

        @Schema(description = "작성자 정보")
        CommentAuthorResponse author,

        @Schema(description = "댓글 내용", example = "좋은 프롬프트네요.")
        String content,

        @Schema(description = "댓글 상태", example = "ACTIVE")
        CommentStatus status,

        @Schema(description = "현재 로그인 사용자가 작성한 댓글인지 여부", example = "true")
        boolean mine,

        @Schema(description = "프롬프트 작성자가 작성한 댓글인지 여부", example = "false")
        boolean promptAuthor,

        @Schema(description = "작성 시각", example = "2026-07-23T01:30:00Z")
        Instant createdAt,

        @Schema(description = "수정 시각", example = "2026-07-23T01:30:00Z")
        Instant updatedAt,

        @Schema(description = "작성 시간 내림차순으로 정렬된 대댓글 목록")
        List<CommentReplyResponse> replies
) {
}
