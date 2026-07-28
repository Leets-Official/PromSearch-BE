package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.infrastructure.persistence.entity.CommentJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<CommentJpaEntity, Long> {

    List<CommentJpaEntity> findAllByPostIdOrderByCreatedAtDesc(Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from CommentJpaEntity comment where comment.id = :commentId")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("commentId") Long commentId);
}
