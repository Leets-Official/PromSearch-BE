package com.promsearch.moderation.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.moderation.application.port.out.commentreport.CommentReportPageResult;
import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.CommentReport.CommentReportId;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.PostReport.PostReportId;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostReportQueryServiceTest {

    private final FakeLoadPostReportPort loadPostReportPort = new FakeLoadPostReportPort();
    private final FakeLoadCommentReportPort loadCommentReportPort = new FakeLoadCommentReportPort();
    private final PostReportQueryService postReportQueryService =
            new PostReportQueryService(loadPostReportPort, loadCommentReportPort);

    @Test
    void searchReportsMapsPostReportContentAndTotal() {
        PostReport report = PostReport.reconstruct(
                new PostReportId(1L), 5L, 10L, ReportReason.SPAM, "설명",
                ReportStatus.PENDING, Instant.now()
        );
        loadPostReportPort.result = new ReportPageResult(List.of(report), 1L);

        ReportPageInfo pageInfo = postReportQueryService.searchReports(
                SearchReportsQuery.of(ReportTargetType.POST, ReportStatus.PENDING, null, 0, 20)
        );

        assertThat(pageInfo.totalElements()).isEqualTo(1L);
        assertThat(pageInfo.content()).hasSize(1);
        assertThat(pageInfo.content().get(0).targetType()).isEqualTo(ReportTargetType.POST);
        assertThat(pageInfo.content().get(0).reporterId()).isEqualTo(5L);
    }

    @Test
    void searchReportsForwardsSearchTermToPort() {
        loadPostReportPort.result = new ReportPageResult(List.of(), 0L);

        postReportQueryService.searchReports(
                SearchReportsQuery.of(ReportTargetType.POST, null, "도배", 0, 20)
        );

        assertThat(loadPostReportPort.lastQ).isEqualTo("도배");
    }

    @Test
    void searchReportsMapsCommentReportContentAndTotal() {
        CommentReport report = CommentReport.reconstruct(
                new CommentReportId(1L), 5L, 20L, ReportReason.SPAM, "설명",
                ReportStatus.PENDING, Instant.now()
        );
        loadCommentReportPort.result = new CommentReportPageResult(List.of(report), 1L);

        ReportPageInfo pageInfo = postReportQueryService.searchReports(
                SearchReportsQuery.of(ReportTargetType.COMMENT, ReportStatus.PENDING, null, 0, 20)
        );

        assertThat(pageInfo.totalElements()).isEqualTo(1L);
        assertThat(pageInfo.content()).hasSize(1);
        assertThat(pageInfo.content().get(0).targetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(pageInfo.content().get(0).targetId()).isEqualTo(20L);
    }

    private static class FakeLoadPostReportPort implements LoadPostReportPort {

        private ReportPageResult result = new ReportPageResult(List.of(), 0L);
        private String lastQ;

        @Override
        public PostReport getById(Long reportId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReportPageResult search(ReportStatus status, String q, int page, int size) {
            this.lastQ = q;
            return result;
        }
    }

    private static class FakeLoadCommentReportPort implements LoadCommentReportPort {

        private CommentReportPageResult result = new CommentReportPageResult(List.of(), 0L);

        @Override
        public CommentReport getById(Long reportId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommentReportPageResult search(ReportStatus status, String q, int page, int size) {
            return result;
        }
    }
}
