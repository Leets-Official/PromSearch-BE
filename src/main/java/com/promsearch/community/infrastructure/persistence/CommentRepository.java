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

    boolean existsByIdAndStatus(Long id, CommentStatus status);

    Optional<CommentJpaEntity> findByIdAndPostIdAndParentCommentIdIsNull(Long id, Long postId);

    Optional<CommentJpaEntity> findByIdAndParentCommentIdAndStatus(
            Long id,
            Long parentCommentId,
            CommentStatus status
    );

    @Query("""
            select comment
            from CommentJpaEntity comment
            where comment.postId = :postId
              and comment.parentCommentId is null
              and (
                    comment.status = :activeStatus
                    or (
                        comment.status = :deletedStatus
                        and exists (
                            select reply.id
                            from CommentJpaEntity reply
                            where reply.parentCommentId = comment.id
                              and reply.status = :activeStatus
                        )
                    )
              )
              and (
                    :cursorCreatedAt is null
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
            @Param("activeStatus") CommentStatus activeStatus,
            @Param("deletedStatus") CommentStatus deletedStatus,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select reply
            from CommentJpaEntity reply
            where reply.parentCommentId = :parentCommentId
              and reply.status = :status
              and (
                    :cursorCreatedAt is null
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
            @Param("status") CommentStatus status,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select reply.parentCommentId as parentId, count(reply.id) as replyCount
            from CommentJpaEntity reply
            where reply.parentCommentId in :parentIds
              and reply.status = :status
            group by reply.parentCommentId
            """)
    List<ParentReplyCountProjection> countRepliesByParentIds(
            @Param("parentIds") Collection<Long> parentIds,
            @Param("status") CommentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from CommentJpaEntity comment where comment.id = :commentId")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("commentId") Long commentId);

    interface ParentReplyCountProjection {

        Long getParentId();

        long getReplyCount();
    }
}
