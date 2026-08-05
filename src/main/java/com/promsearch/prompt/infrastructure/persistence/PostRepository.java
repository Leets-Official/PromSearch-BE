package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {

    @Query("""
            select p.id as promptId, p.title as title, p.publishedAt as publishedAt,
                   coalesce(s.viewCount, 0) as viewCount,
                   coalesce(s.likeCount, 0) as recommendCount
              from PostJpaEntity p
              left join p.statistics s
             where p.userId = :userId
               and p.status = :status
               and (:visibility is null or p.visibility = :visibility)
               and p.deletedAt is null
             order by p.publishedAt desc
            """)
    Page<MyPromptSummaryProjection> findMyPromptSummaries(
            @Param("userId") Long userId,
            @Param("status") PromptStatus status,
            @Param("visibility") PromptVisibility visibility,
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

    boolean existsByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id,
            PromptStatus status,
            PromptVisibility visibility
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select post
            from PostJpaEntity post
            where post.id = :postId
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.ACTIVE
              and post.visibility = com.promsearch.prompt.domain.enums.PromptVisibility.PUBLIC
              and post.deletedAt is null
            """)
    Optional<PostJpaEntity> findAccessibleByIdForUpdate(@Param("postId") Long postId);

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

    @Query("""
            select distinct post
            from PostJpaEntity post
            left join fetch post.postTags postTag
            left join fetch postTag.tag
            where post.id = :postId
              and post.deletedAt is null
            """)
    Optional<PostJpaEntity> findForEditById(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"statistics", "postTags", "postTags.tag"})
    Optional<PostJpaEntity> findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id, PromptStatus status, PromptVisibility visibility);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select post
            from PostJpaEntity post
            where post.id = :postId
              and post.deletedAt is null
            """)
    Optional<PostJpaEntity> findByIdForUpdate(@Param("postId") Long postId);
}
