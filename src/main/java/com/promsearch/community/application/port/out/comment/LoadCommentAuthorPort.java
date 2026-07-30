package com.promsearch.community.application.port.out.comment;

import java.util.Map;
import java.util.Set;

public interface LoadCommentAuthorPort {

    CommentAuthorSnapshot getActiveById(Long userId);

    Map<Long, CommentAuthorSnapshot> batchGetByIds(Set<Long> userIds);

    record CommentAuthorSnapshot(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {
    }
}
