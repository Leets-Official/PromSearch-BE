package com.promsearch.community.application.usecase.dto;

import com.promsearch.community.domain.enums.CommentStatus;
import java.time.Instant;

public record CommentReplyInfo(
        Long commentId,
        Long parentCommentId,
        CommentAuthorInfo author,
        String content,
        CommentStatus status,
        boolean mine,
        boolean promptAuthor,
        Instant createdAt,
        Instant updatedAt
) {
}
