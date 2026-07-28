package com.promsearch.moderation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import org.junit.jupiter.api.Test;

class PostReportTest {

    @Test
    void createBuildsPendingReport() {
        PostReport report = PostReport.create(1L, ReportTargetType.POST, 10L, ReportReason.SPAM, "설명");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(report.getTargetId()).isEqualTo(10L);
    }

    @Test
    void createRejectsReasonNotAllowedForCommentTarget() {
        assertThatThrownBy(() -> PostReport.create(1L, ReportTargetType.COMMENT, 10L, ReportReason.COPYRIGHT, null))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_REASON);
    }

    @Test
    void updateStatusAllowsResolvedOrRejected() {
        PostReport report = PostReport.create(1L, ReportTargetType.POST, 10L, ReportReason.SPAM, null);

        PostReport resolved = report.updateStatus(ReportStatus.RESOLVED);

        assertThat(resolved.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void updateStatusRejectsPending() {
        PostReport report = PostReport.create(1L, ReportTargetType.POST, 10L, ReportReason.SPAM, null);

        assertThatThrownBy(() -> report.updateStatus(ReportStatus.PENDING))
                .isInstanceOf(ModerationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ModerationErrorCode.INVALID_REPORT_STATUS);
    }
}
