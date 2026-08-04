package com.promsearch.commerce.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.commerce.domain.PostCopy;
import com.promsearch.commerce.domain.PostCopy.PostCopyId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "post_copies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_copies_user_post",
                columnNames = {"user_id", "post_id"}
        )
)
public class PostCopyJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_copy_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "copied_at", nullable = false, updatable = false)
    private Instant copiedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostCopyJpaEntity(Long postId, Long userId, Instant copiedAt) {
        this.postId = postId;
        this.userId = userId;
        this.copiedAt = copiedAt;
    }

    public static PostCopyJpaEntity from(PostCopy postCopy) {
        return PostCopyJpaEntity.builder()
                .postId(postCopy.getPostId())
                .userId(postCopy.getUserId())
                .copiedAt(postCopy.getCopiedAt())
                .build();
    }

    public PostCopy toDomain() {
        return PostCopy.reconstruct(new PostCopyId(id), postId, userId, copiedAt);
    }

    @PrePersist
    private void setCopiedAt() {
        if (copiedAt == null) {
            copiedAt = Instant.now();
        }
    }
}
