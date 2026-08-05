package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.community.infrastructure.persistence.entity.CommentJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<CommentJpaEntity, Long> {

    @Query("""
            select count(comment) > 0
            from CommentJpaEntity comment
            where comment.id = :commentId
              and comment.status = com.promsearch.community.domain.enums.CommentStatus.ACTIVE
              and comment.deletedAt is null
              and exists (
                    select post.id
                    from PostJpaEntity post
                    where post.id = comment.postId
                      and post.status = com.promsearch.prompt.domain.enums.PromptStatus.ACTIVE
                      and post.visibility = com.promsearch.prompt.domain.enums.PromptVisibility.PUBLIC
                      and post.deletedAt is null
              )
            """)
    boolean existsReportableById(@Param("commentId") Long commentId);

    Optional<CommentJpaEntity> findByIdAndPostIdAndParentCommentIdIsNull(Long id, Long postId);

    Optional<CommentJpaEntity> findByIdAndParentCommentIdAndStatusIn(
            Long id,
            Long parentCommentId,
            Collection<CommentStatus> statuses
    );

    @Query("""
            select comment
            from CommentJpaEntity comment
            where comment.postId = :postId
              and comment.parentCommentId is null
              and (
                    comment.status in :visibleStatuses
                    or (
                        comment.status = :deletedStatus
                        and exists (
                            select reply.id
                            from CommentJpaEntity reply
                            where reply.parentCommentId = comment.id
                              and reply.status in :visibleStatuses
                        )
                    )
              )
              and (
                    cast(:cursorCreatedAt as timestamp) is null
                    or comment.createdAt < :cursorCreatedAt
                    or (
                        comment.createdAt = :cursorCreatedAt
                        and comment.id < :cursorId
                    )
              )
            order by comment.createdAt desc, comment.id desc
            """)
    List<CommentJpaEntity> findParentPage(
            @Param("postId") Long postId,
            @Param("visibleStatuses") Collection<CommentStatus> visibleStatuses,
            @Param("deletedStatus") CommentStatus deletedStatus,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select reply
            from CommentJpaEntity reply
            where reply.parentCommentId = :parentCommentId
              and reply.status in :visibleStatuses
              and (
                    cast(:cursorCreatedAt as timestamp) is null
                    or reply.createdAt > :cursorCreatedAt
                    or (
                        reply.createdAt = :cursorCreatedAt
                        and reply.id > :cursorId
                    )
              )
            order by reply.createdAt asc, reply.id asc
            """)
    List<CommentJpaEntity> findReplyPage(
            @Param("parentCommentId") Long parentCommentId,
            @Param("visibleStatuses") Collection<CommentStatus> visibleStatuses,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select reply.parentCommentId as parentId, count(reply.id) as replyCount
            from CommentJpaEntity reply
            where reply.parentCommentId in :parentIds
              and reply.status in :visibleStatuses
            group by reply.parentCommentId
            """)
    List<ParentReplyCountProjection> countRepliesByParentIds(
            @Param("parentIds") Collection<Long> parentIds,
            @Param("visibleStatuses") Collection<CommentStatus> visibleStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from CommentJpaEntity comment where comment.id = :commentId")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("commentId") Long commentId);

    @Query("""
            select comment.id as commentId, comment.content as content, comment.userId as authorId,
                   (select u.nickname from UserJpaEntity u where u.id = comment.userId) as authorNickname,
                   (comment.status = com.promsearch.community.domain.enums.CommentStatus.DELETED
                        or comment.deletedAt is not null) as deleted
            from CommentJpaEntity comment
            where comment.id in :commentIds
            """)
    List<CommentReportTargetSummaryProjection> findReportTargetSummaries(
            @Param("commentIds") Collection<Long> commentIds
    );

    interface ParentReplyCountProjection {

        Long getParentId();

        long getReplyCount();
    }

    interface CommentReportTargetSummaryProjection {

        Long getCommentId();

        String getContent();

        Long getAuthorId();

        String getAuthorNickname();

        boolean isDeleted();
    }
}
