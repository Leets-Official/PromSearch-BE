package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.commerce.infrastructure.persistence.entity.PostUnlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostUnlockRepository extends JpaRepository<PostUnlockJpaEntity, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
