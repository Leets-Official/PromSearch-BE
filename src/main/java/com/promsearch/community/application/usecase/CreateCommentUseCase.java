package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CreateCommentCommand;

public interface CreateCommentUseCase {

    CommentInfo createComment(CreateCommentCommand command);
}
