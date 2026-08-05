package com.promsearch.user.application.port.out.graderequest;

import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;

public interface LoadGradeRequestSummaryPort {

    GradeRequestSummaryInfo getById(Long gradeRequestId);
}
