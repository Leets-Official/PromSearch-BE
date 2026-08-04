package com.promsearch.commerce.domain;

import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostCopy {

    private final PostCopyId postCopyId;
    private final Long postId;
    private final Long userId;
    private final Instant copiedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostCopy(PostCopyId postCopyId, Long postId, Long userId, Instant copiedAt) {
        this.postCopyId = postCopyId;
        this.postId = postId;
        this.userId = userId;
        this.copiedAt = copiedAt;
    }

    public static PostCopy create(Long postId, Long userId) {
        validateRequired(postId, userId);
        return PostCopy.builder()
                .postId(postId)
                .userId(userId)
                .copiedAt(Instant.now())
                .build();
    }

    public static PostCopy reconstruct(PostCopyId postCopyId, Long postId, Long userId, Instant copiedAt) {
        validateRequired(postId, userId);
        if (postCopyId == null || copiedAt == null) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_ID);
        }
        return PostCopy.builder()
                .postCopyId(postCopyId)
                .postId(postId)
                .userId(userId)
                .copiedAt(copiedAt)
                .build();
    }

    private static void validateRequired(Long postId, Long userId) {
        if (postId == null || postId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_POST_ID);
        }
        if (userId == null || userId <= 0) {
            throw new CommerceDomainException(CommerceErrorCode.INVALID_USER_ID);
        }
    }

    public record PostCopyId(Long id) {
        public PostCopyId {
            if (id == null || id <= 0) {
                throw new CommerceDomainException(CommerceErrorCode.INVALID_ID);
            }
        }
    }
}
