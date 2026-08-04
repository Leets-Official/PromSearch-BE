package com.promsearch.user.application.port.out.graderequest;

import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;

public interface LoadGradeRequestListPort {

    GradeRequestListInfo list(GradeRequestListQuery query);
}
