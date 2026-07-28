package com.promsearch.community.application.port.out.comment;

import com.promsearch.community.domain.Comment;
import java.util.List;

public interface LoadCommentPort {

    Comment getByIdForUpdate(Long commentId);

    List<Comment> listByPostId(Long postId);
}
