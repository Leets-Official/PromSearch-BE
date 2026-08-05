package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;

public interface ListGradeRequestsUseCase {

    GradeRequestListInfo list(GradeRequestListQuery query);
}
