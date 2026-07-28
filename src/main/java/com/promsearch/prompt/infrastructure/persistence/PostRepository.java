package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {

    Page<PostJpaEntity> findByUserIdAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
            Long userId,
            PromptStatus status,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(s.viewCount), 0) as totalViews,
                   coalesce(sum(s.likeCount), 0) as totalRecommends,
                   coalesce(sum(s.copyCount), 0) as totalCopies
            from PostJpaEntity p
            join p.statistics s
            where p.userId = :userId and p.deletedAt is null
            """)
    PromptInsightProjection sumInsightsByUserId(@Param("userId") Long userId);
}
