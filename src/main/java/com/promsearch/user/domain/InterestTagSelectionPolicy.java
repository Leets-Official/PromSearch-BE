package com.promsearch.user.domain;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.HashSet;
import java.util.List;

public final class InterestTagSelectionPolicy {

    public static final int MAX_TAGS_PER_TYPE = 3;

    private InterestTagSelectionPolicy() {
    }

    public static void validate(List<String> jobTags, List<String> taskTags) {
        validateTags(jobTags);
        validateTags(taskTags);
    }

    private static void validateTags(List<String> tags) {
        if (tags.size() > MAX_TAGS_PER_TYPE
                || tags.stream().anyMatch(tag -> tag == null || tag.isBlank())
                || new HashSet<>(tags).size() != tags.size()) {
            throw new UserDomainException(UserErrorCode.INVALID_INTEREST_TAG);
        }
    }
}
