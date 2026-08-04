package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.infrastructure.persistence.entity.PostReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReportJpaEntity, Long> {

    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);
}
