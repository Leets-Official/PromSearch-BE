package com.promsearch.community.application.usecase.dto;

import java.util.List;

public record CommentReplyListInfo(
        List<CommentReplyInfo> replies,
        Long nextCursor,
        boolean hasNext
) {
}
