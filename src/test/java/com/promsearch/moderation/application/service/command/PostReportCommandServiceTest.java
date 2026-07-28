package com.promsearch.moderation.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.PostReport.PostReportId;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostReportCommandServiceTest {

    private FakePostReportRepository repository;
    private PostReportCommandService postReportCommandService;

    @BeforeEach
    void setUp() {
        repository = new FakePostReportRepository();
        postReportCommandService = new PostReportCommandService(repository, repository);
    }

    @Test
    void updateStatusResolvesReport() {
        repository.save(testReport(1L, ReportStatus.PENDING));

        ReportInfo reportInfo = postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(1L, ReportStatus.RESOLVED)
        );

        assertThat(reportInfo.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(repository.reports.get(1L).getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void updateStatusRejectsPending() {
        repository.save(testReport(1L, ReportStatus.PENDING));

        assertThatThrownBy(() -> postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(1L, ReportStatus.PENDING)
        ))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_STATUS);
    }

    @Test
    void updateStatusRejectsMissingReport() {
        assertThatThrownBy(() -> postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(999L, ReportStatus.RESOLVED)
        ))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.REPORT_NOT_FOUND);
    }

    private PostReport testReport(Long reportId, ReportStatus status) {
        return PostReport.reconstruct(
                new PostReportId(reportId), 5L, ReportTargetType.POST, 10L, ReportReason.SPAM, "설명",
                status, Instant.now()
        );
    }

    private static class FakePostReportRepository implements LoadPostReportPort, SavePostReportPort {

        private final Map<Long, PostReport> reports = new HashMap<>();

        void save(PostReport report) {
            reports.put(report.getPostReportId().id(), report);
        }

        @Override
        public PostReport getById(Long reportId) {
            PostReport report = reports.get(reportId);
            if (report == null) {
                throw new ModerationDomainException(ModerationErrorCode.REPORT_NOT_FOUND);
            }
            return report;
        }

        @Override
        public ReportPageResult search(ReportTargetType targetType, ReportStatus status, int page, int size) {
            return new ReportPageResult(List.copyOf(reports.values()), reports.size());
        }

        @Override
        public PostReport update(PostReport postReport) {
            reports.put(postReport.getPostReportId().id(), postReport);
            return postReport;
        }
    }
}
