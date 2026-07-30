package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.CommentReplyListInfo;
import com.promsearch.community.application.usecase.dto.GetCommentRepliesQuery;

public interface GetCommentRepliesUseCase {

    CommentReplyListInfo getReplies(GetCommentRepliesQuery query);
}
