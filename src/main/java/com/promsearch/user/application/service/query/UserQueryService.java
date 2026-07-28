package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.usecase.GetUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService implements GetUserProfileUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public UserInfo getMyProfile(Long userId) {
        User user = loadUserPort.getById(userId);
        return UserInfo.from(user);
    }
}
