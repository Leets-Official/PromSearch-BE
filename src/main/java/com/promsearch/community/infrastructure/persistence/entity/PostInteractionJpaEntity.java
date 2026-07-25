package com.promsearch.community.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.PostInteraction.PostInteractionId;
import com.promsearch.community.domain.enums.InteractionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "post_interactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_interactions_user_post_type",
                columnNames = {"user_id", "post_id", "interaction_type"}
        )
)
public class PostInteractionJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private InteractionType interactionType;

    @Builder(access = AccessLevel.PRIVATE)
    private PostInteractionJpaEntity(Long userId, Long postId, InteractionType interactionType) {
        this.userId = userId;
        this.postId = postId;
        this.interactionType = interactionType;
    }

    public static PostInteractionJpaEntity create(Long userId, Long postId, InteractionType interactionType) {
        return PostInteractionJpaEntity.builder()
                .userId(userId)
                .postId(postId)
                .interactionType(interactionType)
                .build();
    }

    public PostInteraction toDomain() {
        return PostInteraction.reconstruct(
                new PostInteractionId(id),
                userId,
                postId,
                interactionType,
                getCreatedAt()
        );
    }
}
