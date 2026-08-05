package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptCommentRepository extends JpaRepository<PostJpaEntity, Long> {

    Optional<PostJpaEntity> findByIdAndStatusAndDeletedAtIsNull(Long id, PromptStatus status);

    Optional<PostJpaEntity> findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            Long id,
            PromptStatus status,
            PromptVisibility visibility
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from PostJpaEntity post where post.id = :postId")
    Optional<PostJpaEntity> findByIdForUpdate(@Param("postId") Long postId);

    @Query("""
            select post.id as postId, post.title as title, post.userId as authorId,
                   (select u.nickname from UserJpaEntity u where u.id = post.userId) as authorNickname,
                   (post.status = com.promsearch.prompt.domain.enums.PromptStatus.DELETED
                        or post.deletedAt is not null) as deleted
            from PostJpaEntity post
            where post.id in :postIds
            """)
    List<PostReportTargetSummaryProjection> findReportTargetSummaries(@Param("postIds") Collection<Long> postIds);

    interface PostReportTargetSummaryProjection {

        Long getPostId();

        String getTitle();

        Long getAuthorId();

        String getAuthorNickname();

        boolean isDeleted();
    }
}
