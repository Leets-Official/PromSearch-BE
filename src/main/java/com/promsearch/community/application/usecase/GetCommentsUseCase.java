package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;

public interface GetCommentsUseCase {

    CommentListInfo getComments(GetCommentsQuery query);
}
