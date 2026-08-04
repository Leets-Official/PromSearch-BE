package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.infrastructure.persistence.entity.CommentReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReportJpaEntity, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);
}
