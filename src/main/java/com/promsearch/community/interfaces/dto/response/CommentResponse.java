package com.promsearch.community.interfaces.dto.response;

import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.domain.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "최상위 댓글 응답")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "101")
        Long commentId,

        @Schema(description = "최상위 댓글이므로 항상 null", nullable = true)
        Long parentCommentId,

        @Schema(description = "작성자 정보. 삭제된 댓글이면 null", nullable = true)
        CommentAuthorResponse author,

        @Schema(description = "댓글 내용")
        String content,

        @Schema(description = "댓글 상태", example = "ACTIVE")
        CommentStatus status,

        @Schema(description = "현재 사용자가 작성한 댓글인지 여부")
        boolean mine,

        @Schema(description = "프롬프트 작성자가 작성한 댓글인지 여부")
        boolean promptAuthor,

        @Schema(description = "작성 시각")
        Instant createdAt,

        @Schema(description = "수정 시각")
        Instant updatedAt,

        @Schema(description = "삭제되지 않은 대댓글 개수", example = "3")
        long replyCount
) {
    public static CommentResponse from(CommentInfo info) {
        return new CommentResponse(
                info.commentId(),
                info.parentCommentId(),
                info.author() == null ? null : CommentAuthorResponse.from(info.author()),
                info.content(),
                info.status(),
                info.mine(),
                info.promptAuthor(),
                info.createdAt(),
                info.updatedAt(),
                info.replyCount()
        );
    }
}
