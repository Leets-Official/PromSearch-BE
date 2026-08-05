package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.domain.Comment;
import com.promsearch.community.infrastructure.persistence.entity.CommentJpaEntity;
import com.promsearch.moderation.application.port.out.target.HideCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.ReportTargetSummary;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportTargetPersistenceAdapter implements
        LoadCommentReportTargetPort,
        LoadCommentReportTargetSummaryPort,
        HideCommentReportTargetPort {

    private static final int MAX_TARGET_CONTENT_LENGTH = 200;

    private final CommentRepository commentRepository;

    @Override
    public boolean exists(Long commentId) {
        return commentRepository.existsReportableById(commentId);
    }

    @Override
    public List<ReportTargetSummary> list(Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return List.of();
        }
        return commentRepository.findReportTargetSummaries(commentIds).stream()
                .map(row -> new ReportTargetSummary(
                        row.getCommentId(),
                        truncate(row.getContent()),
                        row.getAuthorId(),
                        row.getAuthorNickname(),
                        row.isDeleted()
                ))
                .toList();
    }

    @Override
    public void hide(Long commentId) {
        CommentJpaEntity entity = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new ModerationDomainException(ModerationErrorCode.REPORT_TARGET_NOT_FOUND));
        Comment comment = entity.toDomain();
        comment.hide();
        entity.updateFrom(comment);
        commentRepository.flush();
    }

    private String truncate(String content) {
        if (content == null || content.length() <= MAX_TARGET_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_TARGET_CONTENT_LENGTH) + "...";
    }
}
