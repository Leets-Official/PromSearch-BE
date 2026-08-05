package com.promsearch.moderation.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.moderation.application.port.out.commentreport.CommentReportPageResult;
import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.commentreport.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.application.port.out.target.HideCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.HidePostReportTargetPort;
import com.promsearch.moderation.application.port.out.target.ReportTargetSummary;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.CommentReport.CommentReportId;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.PostReport.PostReportId;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostReportCommandServiceTest {

    private FakePostReportRepository postReportRepository;
    private FakeCommentReportRepository commentReportRepository;
    private FakeHidePostReportTargetPort hidePostReportTargetPort;
    private FakeHideCommentReportTargetPort hideCommentReportTargetPort;
    private PostReportCommandService postReportCommandService;

    @BeforeEach
    void setUp() {
        postReportRepository = new FakePostReportRepository();
        commentReportRepository = new FakeCommentReportRepository();
        hidePostReportTargetPort = new FakeHidePostReportTargetPort();
        hideCommentReportTargetPort = new FakeHideCommentReportTargetPort();
        postReportCommandService = new PostReportCommandService(
                postReportRepository, postReportRepository, commentReportRepository, commentReportRepository,
                hidePostReportTargetPort, hideCommentReportTargetPort,
                ids -> List.of(), ids -> List.<ReportTargetSummary>of()
        );
    }

    @Test
    void updateStatusResolvesPostReport() {
        postReportRepository.save(testPostReport(1L, ReportStatus.PENDING));

        ReportInfo reportInfo = postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(1L, ReportTargetType.POST, ReportStatus.RESOLVED)
        );

        assertThat(reportInfo.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(postReportRepository.reports.get(1L).getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(hidePostReportTargetPort.hiddenPostIds).containsExactly(10L);
    }

    @Test
    void updateStatusResolvesCommentReport() {
        commentReportRepository.save(testCommentReport(1L, ReportStatus.PENDING));

        ReportInfo reportInfo = postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(1L, ReportTargetType.COMMENT, ReportStatus.RESOLVED)
        );

        assertThat(reportInfo.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(commentReportRepository.reports.get(1L).getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(hideCommentReportTargetPort.hiddenCommentIds).containsExactly(20L);
    }

    @Test
    void updateStatusRejectsPending() {
        postReportRepository.save(testPostReport(1L, ReportStatus.PENDING));

        assertThatThrownBy(() -> postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(1L, ReportTargetType.POST, ReportStatus.PENDING)
        ))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_STATUS);
    }

    @Test
    void updateStatusRejectsMissingReport() {
        assertThatThrownBy(() -> postReportCommandService.updateStatus(
                UpdateReportStatusCommand.of(999L, ReportTargetType.POST, ReportStatus.RESOLVED)
        ))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.REPORT_NOT_FOUND);
    }

    private PostReport testPostReport(Long reportId, ReportStatus status) {
        return PostReport.reconstruct(
                new PostReportId(reportId), 5L, 10L, ReportReason.SPAM, "설명",
                status, Instant.now()
        );
    }

    private CommentReport testCommentReport(Long reportId, ReportStatus status) {
        return CommentReport.reconstruct(
                new CommentReportId(reportId), 5L, 20L, ReportReason.SPAM, "설명",
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
        public ReportPageResult search(ReportStatus status, int page, int size) {
            return new ReportPageResult(List.copyOf(reports.values()), reports.size());
        }

        @Override
        public PostReport update(PostReport postReport) {
            reports.put(postReport.getPostReportId().id(), postReport);
            return postReport;
        }
    }

    private static class FakeCommentReportRepository implements LoadCommentReportPort, SaveCommentReportPort {

        private final Map<Long, CommentReport> reports = new HashMap<>();

        void save(CommentReport report) {
            reports.put(report.getCommentReportId().id(), report);
        }

        @Override
        public CommentReport getById(Long reportId) {
            CommentReport report = reports.get(reportId);
            if (report == null) {
                throw new ModerationDomainException(ModerationErrorCode.REPORT_NOT_FOUND);
            }
            return report;
        }

        @Override
        public CommentReportPageResult search(ReportStatus status, int page, int size) {
            return new CommentReportPageResult(List.copyOf(reports.values()), reports.size());
        }

        @Override
        public CommentReport update(CommentReport commentReport) {
            reports.put(commentReport.getCommentReportId().id(), commentReport);
            return commentReport;
        }
    }

    private static class FakeHidePostReportTargetPort implements HidePostReportTargetPort {

        private final List<Long> hiddenPostIds = new ArrayList<>();

        @Override
        public void hide(Long postId) {
            hiddenPostIds.add(postId);
        }
    }

    private static class FakeHideCommentReportTargetPort implements HideCommentReportTargetPort {

        private final List<Long> hiddenCommentIds = new ArrayList<>();

        @Override
        public void hide(Long commentId) {
            hiddenCommentIds.add(commentId);
        }
    }
}
