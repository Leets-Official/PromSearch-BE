package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.entity.PostInteractionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostInteractionRepository extends JpaRepository<PostInteractionJpaEntity, Long> {
    boolean existsByUserIdAndPostIdAndInteractionType(Long userId, Long postId, InteractionType type);
}
