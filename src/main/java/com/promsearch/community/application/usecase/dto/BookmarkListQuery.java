package com.promsearch.community.application.usecase.dto;

import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.prompt.domain.enums.PromptOutputType;

public record BookmarkListQuery(
        Long userId,
        Long taskTagId,
        Long aiModelTagId,
        PromptOutputType outputType,
        int page,
        int size
) {

    public BookmarkListQuery {
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_USER_ID);
        }
        if (taskTagId != null && taskTagId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_ID);
        }
        if (aiModelTagId != null && aiModelTagId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_ID);
        }
        if (page < 0
                || size <= 0
                || size > 50
                || (long) page * size > Integer.MAX_VALUE) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_BOOKMARK_PAGE);
        }
    }
}
