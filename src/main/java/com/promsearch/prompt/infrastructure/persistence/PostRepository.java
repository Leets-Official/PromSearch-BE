package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {

    boolean existsByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id,
            PromptStatus status,
            PromptVisibility visibility
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct post
            from PostJpaEntity post
            left join fetch post.postTags postTag
            left join fetch postTag.tag
            where post.userId = :userId
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.DRAFT
              and post.deletedAt is null
            """)
    Optional<PostJpaEntity> findDraftByUserIdForUpdate(@Param("userId") Long userId);

    @Query("""
            select distinct post
            from PostJpaEntity post
            left join fetch post.postTags postTag
            left join fetch postTag.tag
            where post.userId = :userId
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.DRAFT
              and post.deletedAt is null
            """)
    Optional<PostJpaEntity> findDraftByUserId(@Param("userId") Long userId);

    @Query("""
            select post.id
            from PostJpaEntity post
            where post.userId = :userId
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.DRAFT
              and post.deletedAt is null
            """)
    Optional<Long> findDraftIdByUserId(@Param("userId") Long userId);
}
