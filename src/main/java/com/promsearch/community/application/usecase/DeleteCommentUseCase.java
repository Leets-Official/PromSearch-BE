package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.DeleteCommentCommand;

public interface DeleteCommentUseCase {

    void deleteComment(DeleteCommentCommand command);
}
