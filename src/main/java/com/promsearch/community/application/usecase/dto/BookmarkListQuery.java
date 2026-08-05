package com.promsearch.community.application.usecase.dto;

import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.util.LinkedHashSet;
import java.util.List;

public record BookmarkListQuery(
        Long userId,
        List<Long> taskTagIds,
        List<Long> aiModelTagIds,
        List<PromptOutputType> outputTypes,
        int page,
        int size
) {

    public static final int MAX_FILTER_TAGS = 10;

    public BookmarkListQuery {
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_INTERACTION_USER_ID);
        }
        taskTagIds = normalizeTagIds(taskTagIds);
        aiModelTagIds = normalizeTagIds(aiModelTagIds);
        outputTypes = normalizeOutputTypes(outputTypes);
        if (page < 0
                || size <= 0
                || size > 50
                || (long) page * size > Integer.MAX_VALUE) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_BOOKMARK_PAGE);
        }
    }

    private static List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueTagIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId == null || tagId <= 0) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_BOOKMARK_FILTER);
            }
            uniqueTagIds.add(tagId);
        }
        if (uniqueTagIds.size() > MAX_FILTER_TAGS) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_BOOKMARK_FILTER);
        }
        return List.copyOf(uniqueTagIds);
    }

    private static List<PromptOutputType> normalizeOutputTypes(List<PromptOutputType> outputTypes) {
        if (outputTypes == null || outputTypes.isEmpty()) {
            return List.of();
        }
        if (outputTypes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_BOOKMARK_FILTER);
        }
        return List.copyOf(new LinkedHashSet<>(outputTypes));
    }
}
