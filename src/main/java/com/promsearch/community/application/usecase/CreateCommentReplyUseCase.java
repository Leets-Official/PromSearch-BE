package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.application.usecase.dto.CreateCommentReplyCommand;

public interface CreateCommentReplyUseCase {

    CommentReplyInfo createReply(CreateCommentReplyCommand command);
}
