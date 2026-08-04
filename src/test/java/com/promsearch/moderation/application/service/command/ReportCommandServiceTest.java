package com.promsearch.moderation.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.moderation.application.port.out.report.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.report.SavePostReportPort;
import com.promsearch.moderation.application.port.out.target.IncreasePostReportCountPort;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetPort;
import com.promsearch.moderation.application.usecase.dto.CreateCommentReportCommand;
import com.promsearch.moderation.application.usecase.dto.CreatePostReportCommand;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import com.promsearch.moderation.domain.enums.ReportReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceTest {

    @Mock SavePostReportPort savePostReportPort;
    @Mock SaveCommentReportPort saveCommentReportPort;
    @Mock LoadPostReportTargetPort loadPostReportTargetPort;
    @Mock LoadCommentReportTargetPort loadCommentReportTargetPort;
    @Mock IncreasePostReportCountPort increasePostReportCountPort;

    private ReportCommandService service;

    @BeforeEach
    void setUp() {
        service = new ReportCommandService(
                savePostReportPort,
                saveCommentReportPort,
                loadPostReportTargetPort,
                loadCommentReportTargetPort,
                increasePostReportCountPort
        );
    }

    @Test
    void createsPostReportThenIncreasesCount() {
        when(loadPostReportTargetPort.exists(10L)).thenReturn(true);

        service.create(new CreatePostReportCommand(1L, 10L, ReportReason.SPAM, " spam "));

        InOrder order = inOrder(savePostReportPort, increasePostReportCountPort);
        order.verify(savePostReportPort).save(org.mockito.ArgumentMatchers.argThat(
                report -> report.getDescription().equals("spam")));
        order.verify(increasePostReportCountPort).increase(10L);
    }

    @Test
    void rejectsDuplicatePostReport() {
        when(loadPostReportTargetPort.exists(10L)).thenReturn(true);
        when(savePostReportPort.existsByReporterIdAndPostId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreatePostReportCommand(1L, 10L, ReportReason.SPAM, "spam")))
                .isInstanceOf(ModerationDomainException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ModerationDomainException) exception).getBaseCode())
                        .isEqualTo(ModerationErrorCode.ALREADY_REPORTED));

        verify(increasePostReportCountPort, never()).increase(10L);
    }

    @Test
    void createsCommentReportWithoutChangingPostCount() {
        when(loadCommentReportTargetPort.exists(20L)).thenReturn(true);

        service.create(new CreateCommentReportCommand(
                1L, 20L, ReportReason.INAPPROPRIATE, "abuse"));

        verify(saveCommentReportPort).save(org.mockito.ArgumentMatchers.argThat(
                report -> report.getCommentId().equals(20L)));
        verify(increasePostReportCountPort, never()).increase(org.mockito.ArgumentMatchers.anyLong());
    }
}
