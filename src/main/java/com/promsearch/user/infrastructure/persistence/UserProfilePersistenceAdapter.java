package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.user.application.port.out.UserProfileStats;
import com.promsearch.user.application.port.out.UserProfileStatsReader;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfilePersistenceAdapter implements UserProfileStatsReader {

    private static final Set<PromptContentType> PUBLIC_PROFILE_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    private final EntityManager entityManager;

    @Override
    public UserProfileStats getByUserId(Long userId) {
        /*
         * Public profiles should show only data that is already eligible for public listing.
         * If a separate author/private visibility column is added later, add that PUBLIC
         * condition here so private posts do not affect another user's profile stats.
         */
        Object[] row = entityManager.createQuery("""
                        select count(p.id),
                               coalesce(sum(s.likeCount), 0),
                               coalesce(sum(s.viewCount), 0)
                        from PostJpaEntity p
                        left join p.statistics s
                        where p.userId = :userId
                          and p.status = :activeStatus
                          and p.deletedAt is null
                          and p.contentType in :contentTypes
                        """, Object[].class)
                .setParameter("userId", userId)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", PUBLIC_PROFILE_CONTENT_TYPES)
                .getSingleResult();

        if (row == null) {
            return UserProfileStats.empty();
        }

        return new UserProfileStats(count(row[0]), count(row[1]), count(row[2]));
    }

    private long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
