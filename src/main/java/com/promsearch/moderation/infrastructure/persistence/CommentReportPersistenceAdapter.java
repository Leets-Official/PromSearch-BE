package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.commentreport.CommentReportPageResult;
import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.commentreport.SaveCommentReportPort;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.moderation.infrastructure.persistence.entity.CommentReportJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportPersistenceAdapter implements LoadCommentReportPort, SaveCommentReportPort {

    private final CommentReportRepository commentReportRepository;

    @Override
    public CommentReport getById(Long reportId) {
        return getJpaEntity(reportId).toDomain();
    }

    @Override
    public CommentReportPageResult search(ReportStatus status, int page, int size) {
        Page<CommentReportJpaEntity> result = commentReportRepository.search(
                status,
                PageRequest.of(page, size)
        );

        List<CommentReport> content = result.getContent().stream()
                .map(CommentReportJpaEntity::toDomain)
                .toList();

        return new CommentReportPageResult(content, result.getTotalElements());
    }

    @Override
    public CommentReport update(CommentReport commentReport) {
        CommentReportJpaEntity jpaEntity = getJpaEntity(commentReport.getCommentReportId().id());
        jpaEntity.updateStatus(commentReport.getStatus());
        commentReportRepository.flush();
        return jpaEntity.toDomain();
    }

    private CommentReportJpaEntity getJpaEntity(Long reportId) {
        return commentReportRepository.findById(reportId)
                .orElseThrow(() -> new ModerationDomainException(ModerationErrorCode.REPORT_NOT_FOUND));
    }
}
