package com.promsearch.moderation.application.service.command;

import com.promsearch.moderation.application.port.out.report.SaveCommentReportPort;
import com.promsearch.moderation.application.port.out.report.SavePostReportPort;
import com.promsearch.moderation.application.port.out.target.IncreasePostReportCountPort;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetPort;
import com.promsearch.moderation.application.usecase.CreateCommentReportUseCase;
import com.promsearch.moderation.application.usecase.CreatePostReportUseCase;
import com.promsearch.moderation.application.usecase.dto.CreateCommentReportCommand;
import com.promsearch.moderation.application.usecase.dto.CreatePostReportCommand;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.exception.ModerationDomainException;
import com.promsearch.moderation.domain.exception.ModerationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandService implements CreatePostReportUseCase, CreateCommentReportUseCase {

    private final SavePostReportPort savePostReportPort;
    private final SaveCommentReportPort saveCommentReportPort;
    private final LoadPostReportTargetPort loadPostReportTargetPort;
    private final LoadCommentReportTargetPort loadCommentReportTargetPort;
    private final IncreasePostReportCountPort increasePostReportCountPort;

    @Override
    public void create(CreatePostReportCommand command) {
        PostReport report = PostReport.create(
                command.reporterId(), command.postId(), command.reason(), command.description());
        if (!loadPostReportTargetPort.exists(command.postId())) {
            throw new ModerationDomainException(ModerationErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        if (savePostReportPort.existsByReporterIdAndPostId(command.reporterId(), command.postId())) {
            throw new ModerationDomainException(ModerationErrorCode.ALREADY_REPORTED);
        }
        savePostReportPort.save(report);
        increasePostReportCountPort.increase(command.postId());
    }

    @Override
    public void create(CreateCommentReportCommand command) {
        CommentReport report = CommentReport.create(
                command.reporterId(), command.commentId(), command.reason(), command.description());
        if (!loadCommentReportTargetPort.exists(command.commentId())) {
            throw new ModerationDomainException(ModerationErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        if (saveCommentReportPort.existsByReporterIdAndCommentId(
                command.reporterId(), command.commentId())) {
            throw new ModerationDomainException(ModerationErrorCode.ALREADY_REPORTED);
        }
        saveCommentReportPort.save(report);
    }
}
