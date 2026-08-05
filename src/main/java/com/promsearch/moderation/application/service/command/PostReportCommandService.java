package com.promsearch.moderation.application.service.command;

import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.commentreport.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.application.port.out.target.HideCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.HidePostReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.ReportTargetSummary;
import com.promsearch.moderation.application.usecase.UpdateReportStatusUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.ReportInfo.TargetSummaryInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostReportCommandService implements UpdateReportStatusUseCase {

    private final LoadPostReportPort loadPostReportPort;
    private final SavePostReportPort savePostReportPort;
    private final LoadCommentReportPort loadCommentReportPort;
    private final SaveCommentReportPort saveCommentReportPort;
    private final HidePostReportTargetPort hidePostReportTargetPort;
    private final HideCommentReportTargetPort hideCommentReportTargetPort;
    private final LoadPostReportTargetSummaryPort loadPostReportTargetSummaryPort;
    private final LoadCommentReportTargetSummaryPort loadCommentReportTargetSummaryPort;

    @Override
    public ReportInfo updateStatus(UpdateReportStatusCommand command) {
        return switch (command.targetType()) {
            case POST -> updatePostReportStatus(command);
            case COMMENT -> updateCommentReportStatus(command);
        };
    }

    private ReportInfo updatePostReportStatus(UpdateReportStatusCommand command) {
        PostReport postReport = loadPostReportPort.getById(command.reportId());
        PostReport updated = savePostReportPort.update(postReport.updateStatus(command.status()));

        if (updated.getStatus() == ReportStatus.RESOLVED) {
            hidePostReportTargetPort.hide(updated.getPostId());
        }

        TargetSummaryInfo targetSummary = singleSummary(
                loadPostReportTargetSummaryPort.list(List.of(updated.getPostId())));
        return ReportInfo.from(updated, targetSummary);
    }

    private ReportInfo updateCommentReportStatus(UpdateReportStatusCommand command) {
        CommentReport commentReport = loadCommentReportPort.getById(command.reportId());
        CommentReport updated = saveCommentReportPort.update(commentReport.updateStatus(command.status()));

        if (updated.getStatus() == ReportStatus.RESOLVED) {
            hideCommentReportTargetPort.hide(updated.getCommentId());
        }

        TargetSummaryInfo targetSummary = singleSummary(
                loadCommentReportTargetSummaryPort.list(List.of(updated.getCommentId())));
        return ReportInfo.from(updated, targetSummary);
    }

    private TargetSummaryInfo singleSummary(List<ReportTargetSummary> summaries) {
        return summaries.stream()
                .findFirst()
                .map(TargetSummaryInfo::from)
                .orElseGet(TargetSummaryInfo::notFound);
    }
}
