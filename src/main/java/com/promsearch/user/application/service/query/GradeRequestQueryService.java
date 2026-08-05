package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestListPort;
import com.promsearch.user.application.usecase.ListGradeRequestsUseCase;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeRequestQueryService implements ListGradeRequestsUseCase {

    private final LoadGradeRequestListPort loadGradeRequestListPort;

    @Override
    public GradeRequestListInfo list(GradeRequestListQuery query) {
        return loadGradeRequestListPort.list(query);
    }
}
