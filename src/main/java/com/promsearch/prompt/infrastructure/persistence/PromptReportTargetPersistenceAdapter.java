package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.target.IncreasePostReportCountPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetPort;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.prompt.domain.enums.PromptStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptReportTargetPersistenceAdapter
        implements LoadPostReportTargetPort, IncreasePostReportCountPort {

    private final PromptCommentRepository promptRepository;
    private final PostStatisticsRepository postStatisticsRepository;

    @Override
    public boolean exists(Long postId) {
        return promptRepository.findByIdAndStatusAndDeletedAtIsNull(postId, PromptStatus.ACTIVE).isPresent();
    }

    @Override
    public void increase(Long postId) {
        if (postStatisticsRepository.incrementReportCount(postId) != 1) {
            throw new ModerationDomainException(ModerationErrorCode.REPORT_COUNT_UPDATE_FAILED);
        }
    }
}
