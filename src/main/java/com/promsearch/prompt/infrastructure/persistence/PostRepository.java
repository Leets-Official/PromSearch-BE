package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {

    Page<PostJpaEntity> findByUserIdAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
            Long userId,
            PromptStatus status,
            Pageable pageable
    );
}
