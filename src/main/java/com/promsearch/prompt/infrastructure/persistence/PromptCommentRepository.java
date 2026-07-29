package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptCommentRepository extends JpaRepository<PostJpaEntity, Long> {

    Optional<PostJpaEntity> findByIdAndStatusAndDeletedAtIsNull(Long id, PromptStatus status);

    Optional<PostJpaEntity> findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id,
            PromptStatus status,
            PromptVisibility visibility
    );
}
