package com.promsearch.community.interfaces.dto;

import com.promsearch.community.domain.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "대댓글 응답")
public record CommentReplyResponse(
        @Schema(description = "대댓글 ID", example = "102")
        Long commentId,

        @Schema(description = "부모 댓글 ID", example = "101")
        Long parentCommentId,

        @Schema(description = "작성자 정보")
        CommentAuthorResponse author,

        @Schema(description = "대댓글 내용", example = "저도 그렇게 생각합니다.")
        String content,

        @Schema(description = "대댓글 상태", example = "ACTIVE")
        CommentStatus status,

        @Schema(description = "현재 로그인 사용자가 작성한 대댓글인지 여부", example = "true")
        boolean mine,

        @Schema(description = "프롬프트 작성자가 작성한 대댓글인지 여부", example = "false")
        boolean promptAuthor,

        @Schema(description = "작성 시각", example = "2026-07-23T03:10:00Z")
        Instant createdAt,

        @Schema(description = "수정 시각", example = "2026-07-23T03:10:00Z")
        Instant updatedAt
) {
}
