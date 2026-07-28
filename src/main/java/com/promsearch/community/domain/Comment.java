package com.promsearch.community.domain;

import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Comment {

    private final CommentId commentId;
    private final Long postId;
    private final Long userId;
    private final Long parentCommentId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private String content;
    private CommentStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Comment(
            CommentId commentId,
            Long postId,
            Long userId,
            String content,
            CommentStatus status,
            Long parentCommentId,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.commentId = commentId;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.status = status;
        this.parentCommentId = parentCommentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Comment create(Long postId, Long userId, String content, Long parentCommentId) {
        validateRequired(postId, userId, content);
        validateParentCommentId(parentCommentId);

        Instant now = Instant.now();
        return Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .status(CommentStatus.ACTIVE)
                .parentCommentId(parentCommentId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Comment reconstruct(
            CommentId commentId,
            Long postId,
            Long userId,
            String content,
            CommentStatus status,
            Long parentCommentId,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        validateRequired(postId, userId, content);
        validateParentCommentId(parentCommentId);
        if (status == null) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND);
        }

        return Comment.builder()
                .commentId(commentId)
                .postId(postId)
                .userId(userId)
                .content(content)
                .status(status)
                .parentCommentId(parentCommentId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public void updateContent(Long requesterId, String content) {
        validateRequester(requesterId);
        validateActive();
        validateContent(content);
        this.content = content;
    }

    public void delete(Long requesterId) {
        validateRequester(requesterId);
        validateActive();
        this.status = CommentStatus.DELETED;
    }

    public void validateCanHaveReply() {
        validateActive();
        if (parentCommentId != null) {
            throw new CommunityDomainException(CommunityErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);
        }
    }

    public boolean isActive() {
        return status == CommentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == CommentStatus.DELETED;
    }

    private static void validateRequired(Long postId, Long userId, String content) {
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_USER_ID);
        }
        if (content == null || content.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_CONTENT);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_CONTENT);
        }
    }

    private void validateRequester(Long requesterId) {
        if (requesterId == null || requesterId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_USER_ID);
        }
        if (!userId.equals(requesterId)) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_OWNED);
        }
    }

    private void validateActive() {
        if (status == CommentStatus.DELETED) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (status != CommentStatus.ACTIVE) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private static void validateParentCommentId(Long parentCommentId) {
        if (parentCommentId != null && parentCommentId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_PARENT_COMMENT);
        }
    }

    public record CommentId(Long id) {
        public CommentId {
            if (id == null || id <= 0) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_ID);
            }
        }
    }
}
