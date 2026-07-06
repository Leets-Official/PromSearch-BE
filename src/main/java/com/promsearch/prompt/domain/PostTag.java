package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostTag {

    private final Long postId;
    private final Long tagId;

    @Builder(access = AccessLevel.PRIVATE)
    private PostTag(Long postId, Long tagId) {
        this.postId = postId;
        this.tagId = tagId;
    }

    public static PostTag create(Long postId, Long tagId) {
        validateRequired(postId, tagId);

        return PostTag.builder()
                .postId(postId)
                .tagId(tagId)
                .build();
    }

    public static PostTag reconstruct(Long postId, Long tagId) {
        validateRequired(postId, tagId);

        return PostTag.builder()
                .postId(postId)
                .tagId(tagId)
                .build();
    }

    private static void validateRequired(Long postId, Long tagId) {
        if (postId == null || postId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        if (tagId == null || tagId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_ID);
        }
    }
}
