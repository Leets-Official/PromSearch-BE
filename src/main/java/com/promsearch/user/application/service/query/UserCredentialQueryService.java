package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCredentialQueryService implements GetUserCredentialUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public Optional<AuthUserInfo> findByEmail(String email) {
        return loadUserPort.findByEmail(email)
                .map(AuthUserInfo::from);
    }

    @Override
    public Optional<AuthUserInfo> findById(Long userId) {
        return loadUserPort.findById(userId)
                .map(AuthUserInfo::from);
    }
}
