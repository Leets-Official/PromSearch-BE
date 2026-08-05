package com.promsearch.user.domain;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.HashSet;
import java.util.List;

public final class InterestTagSelectionPolicy {

    public static final int MAX_TAGS_PER_TYPE = 3;

    private InterestTagSelectionPolicy() {
    }

    public static void validate(List<Long> jobTagIds, List<Long> taskTagIds) {
        validateTagIds(jobTagIds);
        validateTagIds(taskTagIds);
    }

    private static void validateTagIds(List<Long> tagIds) {
        if (tagIds.size() > MAX_TAGS_PER_TYPE
                || tagIds.stream().anyMatch(tagId -> tagId == null || tagId <= 0)
                || new HashSet<>(tagIds).size() != tagIds.size()) {
            throw new UserDomainException(UserErrorCode.INVALID_INTEREST_TAG);
        }
    }
}
