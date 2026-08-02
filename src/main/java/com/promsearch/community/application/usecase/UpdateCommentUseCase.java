package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.UpdateCommentCommand;

public interface UpdateCommentUseCase {

    CommentInfo updateComment(UpdateCommentCommand command);
}
