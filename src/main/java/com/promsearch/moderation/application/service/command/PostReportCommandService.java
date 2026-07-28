package com.promsearch.moderation.application.service.command;

import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.SavePostReportPort;
import com.promsearch.moderation.application.usecase.UpdateReportStatusUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
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

    @Override
    public ReportInfo updateStatus(UpdateReportStatusCommand command) {
        PostReport postReport = loadPostReportPort.getById(command.reportId());
        PostReport updated = postReport.updateStatus(command.status());

        return ReportInfo.from(savePostReportPort.update(updated));
    }
}
