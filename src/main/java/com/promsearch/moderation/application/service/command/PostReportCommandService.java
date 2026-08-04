package com.promsearch.moderation.application.service.command;

import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.commentreport.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.application.usecase.UpdateReportStatusUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.PostReport;
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

    @Override
    public ReportInfo updateStatus(UpdateReportStatusCommand command) {
        return switch (command.targetType()) {
            case POST -> updatePostReportStatus(command);
            case COMMENT -> updateCommentReportStatus(command);
        };
    }

    private ReportInfo updatePostReportStatus(UpdateReportStatusCommand command) {
        PostReport postReport = loadPostReportPort.getById(command.reportId());
        PostReport updated = postReport.updateStatus(command.status());

        return ReportInfo.from(savePostReportPort.update(updated));
    }

    private ReportInfo updateCommentReportStatus(UpdateReportStatusCommand command) {
        CommentReport commentReport = loadCommentReportPort.getById(command.reportId());
        CommentReport updated = commentReport.updateStatus(command.status());

        return ReportInfo.from(saveCommentReportPort.update(updated));
    }
}
