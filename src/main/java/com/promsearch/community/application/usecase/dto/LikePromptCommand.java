package com.promsearch.community.application.usecase.dto;

import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;

public record LikePromptCommand(
        Long userId,
        Long promptId
) {

    public LikePromptCommand {
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_USER_ID);
        }
        if (promptId == null || promptId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_POST_ID);
        }
    }
}
