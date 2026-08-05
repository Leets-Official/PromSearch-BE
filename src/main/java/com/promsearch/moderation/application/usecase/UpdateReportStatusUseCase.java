package com.promsearch.moderation.application.usecase;

import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;

public interface UpdateReportStatusUseCase {

    ReportInfo updateStatus(UpdateReportStatusCommand command);
}
