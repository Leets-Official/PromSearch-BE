package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.persistence.refresh.RevokeRefreshTokenSessionPort;
import com.promsearch.auth.application.usecase.LogoutUseCase;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RevokeRefreshTokenSessionPort revokeRefreshTokenSessionPort;

    @Override
    @Transactional
    public void logout(Long userId) {
        revokeRefreshTokenSessionPort.revokeActiveSessionsByUserId(userId, Instant.now());
        log.info("auth_logout userId={}", userId);
    }
}
