package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.infrastructure.persistence.entity.CommentReportJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReportRepository extends JpaRepository<CommentReportJpaEntity, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    @Query("""
            select r from CommentReportJpaEntity r
            where (:status is null or r.status = :status)
              and (:q is null or exists (
                    select 1 from CommentJpaEntity c
                    where c.id = r.commentId
                      and (lower(c.content) like :q
                           or exists (
                                select 1 from UserJpaEntity u
                                where u.id = c.userId and lower(u.nickname) like :q
                           ))
              ))
            order by r.createdAt desc
            """)
    Page<CommentReportJpaEntity> search(@Param("status") ReportStatus status, @Param("q") String q, Pageable pageable);
}
