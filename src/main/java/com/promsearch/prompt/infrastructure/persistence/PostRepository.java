package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {
    @EntityGraph(attributePaths = {"statistics", "postTags", "postTags.tag"})
    Optional<PostJpaEntity> findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id, PromptStatus status, PromptVisibility visibility);
}
