package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.usecase.CheckNicknameAvailabilityUseCase;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNicknameQueryService implements CheckNicknameAvailabilityUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public NicknameAvailabilityInfo checkAvailability(NicknameAvailabilityQuery query) {
        return NicknameAvailabilityInfo.from(loadUserPort.existsByNickname(query.nickname()));
    }
}
