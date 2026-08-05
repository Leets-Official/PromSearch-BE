package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.user.LoadOriginUserListPort;
import com.promsearch.user.application.usecase.ListOriginUsersUseCase;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OriginUserQueryService implements ListOriginUsersUseCase {

    private final LoadOriginUserListPort loadOriginUserListPort;

    @Override
    public OriginUserListInfo list(OriginUserListQuery query) {
        return loadOriginUserListPort.list(query);
    }
}
