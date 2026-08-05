package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.commerce.application.port.out.prompt.IncreasePromptCopyCountPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort.PromptAccessTarget;
import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptAccessPersistenceAdapter
        implements LoadPromptAccessTargetPort, IncreasePromptCopyCountPort {

    private final PostRepository postRepository;
    private final PostStatisticsRepository postStatisticsRepository;

    @Override
    public PromptAccessTarget getByIdForUpdate(Long promptId) {
        PostJpaEntity post = postRepository.findAccessibleByIdForUpdate(promptId)
                .orElseThrow(() -> new CommerceDomainException(
                        CommerceErrorCode.PROMPT_NOT_ACCESSIBLE));
        return new PromptAccessTarget(
                post.getId(),
                post.getUserId(),
                post.getContentType() == PromptContentType.FREE,
                post.getStatistics().toDomain().getCopyCount()
        );
    }

    @Override
    public void increase(Long promptId) {
        if (postStatisticsRepository.incrementCopyCount(promptId) != 1) {
            throw new CommerceDomainException(CommerceErrorCode.COPY_COUNT_UPDATE_FAILED);
        }
    }
}
