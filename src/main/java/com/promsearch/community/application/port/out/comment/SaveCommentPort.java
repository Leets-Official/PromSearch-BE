package com.promsearch.community.application.port.out.comment;

import com.promsearch.community.domain.Comment;

public interface SaveCommentPort {

    Comment create(Comment comment);

    Comment update(Comment comment);
}
