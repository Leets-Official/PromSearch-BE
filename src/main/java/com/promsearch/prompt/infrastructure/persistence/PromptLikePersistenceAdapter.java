package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.application.port.out.prompt.UpdatePromptLikeCountPort;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptLikePersistenceAdapter implements UpdatePromptLikeCountPort {

    private final PostStatisticsRepository postStatisticsRepository;

    @Override
    public long increase(Long promptId) {
        PostStatisticsJpaEntity statistics = postStatisticsRepository
                .findLikeableByPostIdForUpdate(promptId)
                .orElseThrow(() -> new CommunityDomainException(
                        CommunityErrorCode.INTERACTION_TARGET_NOT_FOUND
                ));
        return statistics.increaseLikeCount();
    }

    @Override
    public long decrease(Long promptId) {
        PostStatisticsJpaEntity statistics = postStatisticsRepository
                .findByPostIdForUpdate(promptId)
                .orElseThrow(() -> new CommunityDomainException(
                        CommunityErrorCode.INTERACTION_TARGET_NOT_FOUND
                ));
        try {
            return statistics.decreaseLikeCount();
        } catch (IllegalStateException exception) {
            throw new CommunityDomainException(CommunityErrorCode.INTERACTION_COUNT_INCONSISTENT);
        }
    }
}
