package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostStatisticsRepository extends JpaRepository<PostStatisticsJpaEntity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PostStatisticsJpaEntity statistics
            set statistics.copyCount = statistics.copyCount + 1
            where statistics.postId = :promptId
            """)
    int incrementCopyCount(@Param("promptId") Long promptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select statistics
            from PostStatisticsJpaEntity statistics
            join statistics.post post
            where statistics.postId = :promptId
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.ACTIVE
              and post.visibility = com.promsearch.prompt.domain.enums.PromptVisibility.PUBLIC
              and post.deletedAt is null
            """)
    Optional<PostStatisticsJpaEntity> findLikeableByPostIdForUpdate(@Param("promptId") Long promptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select statistics
            from PostStatisticsJpaEntity statistics
            where statistics.postId = :promptId
            """)
    Optional<PostStatisticsJpaEntity> findByPostIdForUpdate(@Param("promptId") Long promptId);
}
