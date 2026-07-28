package com.promsearch.community.application.port.out.comment;

public interface LoadCommentTargetPort {

    CommentTargetSnapshot getActiveById(Long postId);

    record CommentTargetSnapshot(
            Long postId,
            Long authorId
    ) {
    }
}
