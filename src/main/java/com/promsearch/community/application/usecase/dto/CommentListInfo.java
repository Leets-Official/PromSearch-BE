package com.promsearch.community.application.usecase.dto;

import java.util.List;

public record CommentListInfo(
        List<CommentInfo> comments
) {
}
