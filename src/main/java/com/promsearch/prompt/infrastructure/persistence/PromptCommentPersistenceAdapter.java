package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.application.port.out.comment.AdjustCommentCountPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptCommentPersistenceAdapter implements LoadCommentTargetPort, AdjustCommentCountPort {

    private final PromptCommentRepository promptCommentRepository;
    private final PromptCommentStatisticsRepository promptCommentStatisticsRepository;

    @Override
    public CommentTargetSnapshot getActiveById(Long postId) {
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        PostJpaEntity post = promptCommentRepository
                .findByIdAndStatusAndDeletedAtIsNull(postId, PromptStatus.ACTIVE)
                .orElseThrow(() -> new CommunityDomainException(
                        CommunityErrorCode.COMMENT_TARGET_PROMPT_NOT_FOUND
                ));
        return new CommentTargetSnapshot(post.getId(), post.getUserId());
    }

    @Override
    public CommentTargetSnapshot getActivePublicById(Long postId) {
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        PostJpaEntity post = promptCommentRepository
                .findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                        postId,
                        PromptStatus.ACTIVE,
                        PromptVisibility.PUBLIC
                )
                .orElseThrow(() -> new CommunityDomainException(
                        CommunityErrorCode.COMMENT_TARGET_PROMPT_NOT_FOUND
                ));
        return new CommentTargetSnapshot(post.getId(), post.getUserId());
    }

    @Override
    public void increment(Long postId) {
        if (promptCommentStatisticsRepository.incrementCommentCount(postId) != 1) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_COUNT_UPDATE_FAILED);
        }
    }

    @Override
    public void decrement(Long postId) {
        if (promptCommentStatisticsRepository.decrementCommentCount(postId) != 1) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_COUNT_UPDATE_FAILED);
        }
    }
}
