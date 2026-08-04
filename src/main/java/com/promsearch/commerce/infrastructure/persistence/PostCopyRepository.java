package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.commerce.infrastructure.persistence.entity.PostCopyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCopyRepository extends JpaRepository<PostCopyJpaEntity, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
