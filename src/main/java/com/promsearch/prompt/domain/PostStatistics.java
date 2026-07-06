package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostStatistics {

    private final Long postId;
    private final long viewCount;
    private final long copyCount;
    private final long likeCount;
    private final long reportCount;
    private final long commentCount;

    @Builder(access = AccessLevel.PRIVATE)
    private PostStatistics(
            Long postId,
            long viewCount,
            long copyCount,
            long likeCount,
            long reportCount,
            long commentCount
    ) {
        this.postId = postId;
        this.viewCount = viewCount;
        this.copyCount = copyCount;
        this.likeCount = likeCount;
        this.reportCount = reportCount;
        this.commentCount = commentCount;
    }

    public static PostStatistics create(Long postId) {
        validatePostId(postId);

        return PostStatistics.builder()
                .postId(postId)
                .viewCount(0)
                .copyCount(0)
                .likeCount(0)
                .reportCount(0)
                .commentCount(0)
                .build();
    }

    public static PostStatistics reconstruct(
            Long postId,
            long viewCount,
            long copyCount,
            long likeCount,
            long reportCount,
            long commentCount
    ) {
        validatePostId(postId);
        validateCounts(viewCount, copyCount, likeCount, reportCount, commentCount);

        return PostStatistics.builder()
                .postId(postId)
                .viewCount(viewCount)
                .copyCount(copyCount)
                .likeCount(likeCount)
                .reportCount(reportCount)
                .commentCount(commentCount)
                .build();
    }

    private static void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
    }

    private static void validateCounts(long... counts) {
        for (long count : counts) {
            if (count < 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
        }
    }
}
