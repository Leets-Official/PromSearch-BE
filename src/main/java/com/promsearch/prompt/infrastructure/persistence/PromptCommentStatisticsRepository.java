package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptCommentStatisticsRepository extends JpaRepository<PostStatisticsJpaEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PostStatisticsJpaEntity statistics
            set statistics.commentCount = statistics.commentCount + 1
            where statistics.postId = :postId
            """)
    int incrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PostStatisticsJpaEntity statistics
            set statistics.commentCount = statistics.commentCount - 1
            where statistics.postId = :postId
              and statistics.commentCount > 0
            """)
    int decrementCommentCount(@Param("postId") Long postId);
}
