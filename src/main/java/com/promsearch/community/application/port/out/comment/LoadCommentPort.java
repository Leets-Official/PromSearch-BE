package com.promsearch.community.application.port.out.comment;

import com.promsearch.community.domain.Comment;
import java.util.List;
import java.util.Map;

public interface LoadCommentPort {

    Comment getById(Long commentId);

    Comment getByIdForUpdate(Long commentId);

    CommentPage listParentPage(Long postId, Long cursor, int size);

    ReplyPage listReplyPage(Long parentCommentId, Long cursor, int size);

    record CommentPage(
            List<Comment> comments,
            Map<Long, Long> replyCounts,
            Long nextCursor,
            boolean hasNext
    ) {
    }

    record ReplyPage(
            List<Comment> replies,
            Long nextCursor,
            boolean hasNext
    ) {
    }
}
