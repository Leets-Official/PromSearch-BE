package com.promsearch.auth.application;

import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.RefreshTokenProvider.RefreshTokenClaims;
import com.promsearch.auth.application.port.out.RefreshTokenSessionRepository;
import com.promsearch.auth.application.port.out.TokenHasher;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthCommandService implements LoginUseCase, ReissueUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final TokenHasher tokenHasher;

    @Override
    @Transactional
    public LoginInfo login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS));

        validatePassword(command.password(), user.getPassword());
        validateActiveUser(user);

        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(user);
        saveRefreshTokenSession(user.getUserId().id(), refreshToken, UUID.randomUUID().toString());

        return LoginInfo.of(
                accessTokenProvider.createAccessToken(user),
                refreshToken.value(),
                accessTokenProvider.getAccessTokenExpirationSeconds(),
                user
        );
    }

    @Override
    @Transactional
    public ReissueInfo reissue(ReissueCommand command) {
        RefreshTokenClaims claims = refreshTokenProvider.parse(command.refreshToken());
        RefreshTokenSession session = refreshTokenSessionRepository
                .findByTokenHashForUpdate(tokenHasher.hash(command.refreshToken()))
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));
        Instant now = Instant.now();
        validateRefreshTokenSession(session, claims, now);

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));

        validateActiveUser(user);
        session.revoke(now);
        refreshTokenSessionRepository.save(session);
        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(user);
        saveRefreshTokenSession(user.getUserId().id(), refreshToken, session.getFamilyId());

        return ReissueInfo.of(
                accessTokenProvider.createAccessToken(user),
                refreshToken.value(),
                accessTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private void saveRefreshTokenSession(Long userId, RefreshToken refreshToken, String familyId) {
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                userId, tokenHasher.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
    }

    private void validateRefreshTokenSession(RefreshTokenSession session, RefreshTokenClaims claims, Instant now) {
        if (!session.getUserId().equals(claims.userId())
                || !session.getExpiresAt().equals(claims.expiresAt())
                || !session.isAvailableAt(now)) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void validateActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}
