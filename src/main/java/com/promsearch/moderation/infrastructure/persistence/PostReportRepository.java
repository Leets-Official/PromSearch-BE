package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.infrastructure.persistence.entity.PostReportJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostReportRepository extends JpaRepository<PostReportJpaEntity, Long> {

    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);

    @Query("""
            select r from PostReportJpaEntity r
            where (:status is null or r.status = :status)
              and (:q is null or exists (
                    select 1 from PostJpaEntity p
                    where p.id = r.postId
                      and (lower(p.title) like :q
                           or exists (
                                select 1 from UserJpaEntity u
                                where u.id = p.userId and lower(u.nickname) like :q
                           ))
              ))
            order by r.createdAt desc
            """)
    Page<PostReportJpaEntity> search(@Param("status") ReportStatus status, @Param("q") String q, Pageable pageable);
}
