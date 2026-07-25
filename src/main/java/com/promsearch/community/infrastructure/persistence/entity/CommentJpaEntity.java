package com.promsearch.community.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.Comment.CommentId;
import com.promsearch.community.domain.enums.CommentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments")
public class CommentJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommentStatus status;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Builder(access = AccessLevel.PRIVATE)
    private CommentJpaEntity(Long postId, Long userId, String content, Long parentCommentId) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.status = CommentStatus.ACTIVE;
    }

    public static CommentJpaEntity create(Long postId, Long userId, String content, Long parentCommentId) {
        return CommentJpaEntity.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .parentCommentId(parentCommentId)
                .build();
    }

    public Comment toDomain() {
        return Comment.reconstruct(
                new CommentId(id),
                postId,
                userId,
                content,
                status,
                parentCommentId,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }
}
