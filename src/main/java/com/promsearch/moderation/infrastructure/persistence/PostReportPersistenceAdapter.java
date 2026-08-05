package com.promsearch.moderation.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.moderation.infrastructure.persistence.entity.PostReportJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReportPersistenceAdapter implements LoadPostReportPort, SavePostReportPort {

    private final PostReportRepository postReportRepository;

    @Override
    public PostReport getById(Long reportId) {
        return getJpaEntity(reportId).toDomain();
    }

    @Override
    public ReportPageResult search(ReportStatus status, int page, int size) {
        Page<PostReportJpaEntity> result = postReportRepository.search(
                status,
                PageRequest.of(page, size)
        );

        List<PostReport> content = result.getContent().stream()
                .map(PostReportJpaEntity::toDomain)
                .toList();

        return new ReportPageResult(content, result.getTotalElements());
    }

    @Override
    public PostReport update(PostReport postReport) {
        PostReportJpaEntity jpaEntity = getJpaEntity(postReport.getPostReportId().id());
        jpaEntity.updateStatus(postReport.getStatus());
        postReportRepository.flush();
        return jpaEntity.toDomain();
    }

    private PostReportJpaEntity getJpaEntity(Long reportId) {
        return postReportRepository.findById(reportId)
                .orElseThrow(() -> new ModerationDomainException(ModerationErrorCode.REPORT_NOT_FOUND));
    }
}
