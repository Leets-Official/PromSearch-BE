package com.promsearch.community.application.port.out.comment;

public interface LoadCommentTargetPort {

    CommentTargetSnapshot getActiveById(Long postId);

    CommentTargetSnapshot getActivePublicById(Long postId);

    record CommentTargetSnapshot(
            Long postId,
            Long authorId
    ) {
    }
}
