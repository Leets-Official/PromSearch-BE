package com.promsearch.user.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestListPort;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeRequestQueryServiceTest {

    @Mock
    private LoadGradeRequestListPort loadGradeRequestListPort;

    @DisplayName("조회 결과를 그대로 반환한다")
    @Test
    void listDelegatesToPort() {
        GradeRequestQueryService service = new GradeRequestQueryService(loadGradeRequestListPort);
        GradeRequestListQuery query = new GradeRequestListQuery(GradeRequestStatus.PENDING, null, 0, 20);
        GradeRequestListInfo info = new GradeRequestListInfo(List.of(), 0, 20, 0, false);
        when(loadGradeRequestListPort.list(query)).thenReturn(info);

        GradeRequestListInfo result = service.list(query);

        assertThat(result).isEqualTo(info);
    }
}
