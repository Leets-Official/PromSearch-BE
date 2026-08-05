package com.promsearch.moderation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import org.junit.jupiter.api.Test;

class CommentReportTest {

    @Test
    void createBuildsPendingReport() {
        CommentReport report = CommentReport.create(1L, 10L, ReportReason.SPAM, "설명");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getCommentId()).isEqualTo(10L);
    }

    @Test
    void createRejectsReasonNotAllowedForCommentTarget() {
        assertThatThrownBy(() -> CommentReport.create(1L, 10L, ReportReason.COPYRIGHT, "설명"))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_REASON);
    }

    @Test
    void updateStatusAllowsResolvedOrRejected() {
        CommentReport report = CommentReport.create(1L, 10L, ReportReason.SPAM, "설명");

        CommentReport resolved = report.updateStatus(ReportStatus.RESOLVED);

        assertThat(resolved.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void updateStatusRejectsPending() {
        CommentReport report = CommentReport.create(1L, 10L, ReportReason.SPAM, "설명");

        assertThatThrownBy(() -> report.updateStatus(ReportStatus.PENDING))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_STATUS);
    }
}
