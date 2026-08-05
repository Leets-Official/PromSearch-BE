package com.promsearch.user.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.user.LoadOriginUserListPort;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OriginUserQueryServiceTest {

    @Mock
    private LoadOriginUserListPort loadOriginUserListPort;

    @DisplayName("조회 결과를 그대로 반환한다")
    @Test
    void listDelegatesToPort() {
        OriginUserQueryService service = new OriginUserQueryService(loadOriginUserListPort);
        OriginUserListQuery query = new OriginUserListQuery(0, 20);
        OriginUserListInfo info = new OriginUserListInfo(List.of(), 0, 20, 0, false);
        when(loadOriginUserListPort.list(query)).thenReturn(info);

        OriginUserListInfo result = service.list(query);

        assertThat(result).isEqualTo(info);
    }
}
