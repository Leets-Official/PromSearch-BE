package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.target.HidePostReportTargetPort;
import com.promsearch.moderation.application.port.out.target.IncreasePostReportCountPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.ReportTargetSummary;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptReportTargetPersistenceAdapter
        implements LoadPostReportTargetPort, IncreasePostReportCountPort,
                   LoadPostReportTargetSummaryPort, HidePostReportTargetPort {

    private static final int MAX_TARGET_CONTENT_LENGTH = 200;

    private final PromptCommentRepository promptRepository;
    private final PostStatisticsRepository postStatisticsRepository;

    @Override
    public boolean exists(Long postId) {
        return promptRepository.findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                postId,
                PromptStatus.ACTIVE,
                PromptVisibility.PUBLIC
        ).isPresent();
    }

    @Override
    public void increase(Long postId) {
        if (postStatisticsRepository.incrementReportCountIfReportable(postId) != 1) {
            throw new ModerationDomainException(ModerationErrorCode.REPORT_COUNT_UPDATE_FAILED);
        }
    }

    @Override
    public List<ReportTargetSummary> list(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return promptRepository.findReportTargetSummaries(postIds).stream()
                .map(row -> new ReportTargetSummary(
                        row.getPostId(),
                        truncate(row.getTitle()),
                        row.getAuthorId(),
                        row.getAuthorNickname(),
                        row.isDeleted()
                ))
                .toList();
    }

    @Override
    public void hide(Long postId) {
        PostJpaEntity post = promptRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ModerationDomainException(ModerationErrorCode.REPORT_TARGET_NOT_FOUND));
        post.hide();
        promptRepository.flush();
    }

    private String truncate(String content) {
        if (content == null || content.length() <= MAX_TARGET_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_TARGET_CONTENT_LENGTH) + "...";
    }
}
