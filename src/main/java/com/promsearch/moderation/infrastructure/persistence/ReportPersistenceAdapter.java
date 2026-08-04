package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.report.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.report.SavePostReportPort;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.moderation.infrastructure.persistence.entity.CommentReportJpaEntity;
import com.promsearch.moderation.infrastructure.persistence.entity.PostReportJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements SavePostReportPort, SaveCommentReportPort {

    private final PostReportRepository postReportRepository;
    private final CommentReportRepository commentReportRepository;

    @Override
    public boolean existsByReporterIdAndPostId(Long reporterId, Long postId) {
        return postReportRepository.existsByReporterIdAndPostId(reporterId, postId);
    }

    @Override
    public void save(PostReport report) {
        try {
            postReportRepository.saveAndFlush(PostReportJpaEntity.from(report));
        } catch (DataIntegrityViolationException exception) {
            throw new ModerationDomainException(ModerationErrorCode.ALREADY_REPORTED);
        }
    }

    @Override
    public boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId) {
        return commentReportRepository.existsByReporterIdAndCommentId(reporterId, commentId);
    }

    @Override
    public void save(CommentReport report) {
        try {
            commentReportRepository.saveAndFlush(CommentReportJpaEntity.from(report));
        } catch (DataIntegrityViolationException exception) {
            throw new ModerationDomainException(ModerationErrorCode.ALREADY_REPORTED);
        }
    }
}
