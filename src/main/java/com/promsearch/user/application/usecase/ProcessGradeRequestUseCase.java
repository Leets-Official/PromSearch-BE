package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.application.usecase.dto.ProcessGradeRequestCommand;

public interface ProcessGradeRequestUseCase {

    GradeRequestSummaryInfo process(ProcessGradeRequestCommand command);
}
