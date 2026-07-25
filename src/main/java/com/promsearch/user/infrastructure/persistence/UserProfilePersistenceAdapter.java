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

    /*
     * 상대 프로필 통계는 공개 프롬프트 목록과 같은 노출 기준을 따라야 합니다.
     * 노출 가능한 콘텐츠 타입을 명시적으로 제한해, 이후 타입이 추가되어도 통계에 임의로 포함되지 않게 합니다.
     */
    private static final Set<PromptContentType> PUBLIC_PROFILE_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    private final EntityManager entityManager;

    @Override
    public UserProfileStats getByUserId(Long userId) {
        /*
         * 사용자 존재 여부와 활성 상태는 이 reader를 호출하기 전에 UserRepository에서 확인합니다.
         * 이 쿼리는 공개 화면에 노출 가능한 프롬프트의 통계만 집계합니다.
         * PromptVisibility가 영속 모델에 연결되면 이 조건에 PUBLIC 필터를 추가하면 됩니다.
         */
        /*
         * 프로필 통계는 실시간 집계 방식으로 계산합니다.
         * 현재 MVP 범위에서는 별도 통계 캐시나 집계 테이블 없이 posts + post_statistics를 직접 합산합니다.
         */
        Object[] row = entityManager.createQuery("""
                        select count(distinct p.id),
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
            /*
             * aggregate query는 보통 결과 row를 반환하지만, 방어적으로 빈 통계를 반환합니다.
             * 호출자는 통계가 없는 사용자도 0 값으로 안전하게 응답할 수 있습니다.
             */
            return UserProfileStats.empty();
        }

        return new UserProfileStats(count(row[0]), count(row[1]), count(row[2]));
    }

    private long count(Object value) {
        // left join된 통계 row가 없거나 sum 결과가 null이면 화면에는 0으로 표시합니다.
        return value == null ? 0L : ((Number) value).longValue();
    }
}
