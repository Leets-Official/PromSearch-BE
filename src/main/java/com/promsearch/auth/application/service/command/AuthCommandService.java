package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.refresh.LoadRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider.RefreshTokenClaims;
import com.promsearch.auth.application.port.out.token.TokenHasher;
import com.promsearch.auth.application.usecase.LoginUseCase;
import com.promsearch.auth.application.usecase.ReissueUseCase;
import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.LoginCommand;
import com.promsearch.auth.application.usecase.dto.LoginInfo;
import com.promsearch.auth.application.usecase.dto.ReissueCommand;
import com.promsearch.auth.application.usecase.dto.ReissueInfo;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthCommandService implements LoginUseCase, ReissueUseCase {

    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final LoadRefreshTokenSessionPort loadRefreshTokenSessionPort;
    private final SaveRefreshTokenSessionPort saveRefreshTokenSessionPort;
    private final TokenHasher tokenHasher;

    @Override
    @Transactional
    public LoginInfo login(LoginCommand command) {
        AuthUserInfo user = getUserCredentialUseCase.findByEmail(command.email())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS));

        validatePassword(command.password(), user.encodedPassword());
        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);

        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, UUID.randomUUID().toString());

        log.info("auth_login_succeeded userId={}", authenticatedUser.userId());

        return LoginInfo.of(
                accessTokenProvider.createAccessToken(authenticatedUser),
                refreshToken.value(),
                accessTokenProvider.getAccessTokenExpirationSeconds(),
                authenticatedUser,
                user.name(),
                user.nickname()
        );
    }

    @Override
    @Transactional
    public ReissueInfo reissue(ReissueCommand command) {
        RefreshTokenClaims claims = refreshTokenProvider.parse(command.refreshToken());
        RefreshTokenSession session = loadRefreshTokenSessionPort
                .findByTokenHashForUpdate(tokenHasher.hash(command.refreshToken()))
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));
        Instant now = Instant.now();
        validateRefreshTokenSession(session, claims, now);

        AuthUserInfo user = getUserCredentialUseCase.findById(claims.userId())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));

        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);
        session.revoke(now);
        saveRefreshTokenSessionPort.save(session);
        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, session.getFamilyId());

        log.info("auth_token_reissued userId={} refreshTokenFamilyId={}",
                authenticatedUser.userId(),
                session.getFamilyId());

        return ReissueInfo.of(
                accessTokenProvider.createAccessToken(authenticatedUser),
                refreshToken.value(),
                accessTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private void saveRefreshTokenSession(Long userId, RefreshToken refreshToken, String familyId) {
        saveRefreshTokenSessionPort.save(RefreshTokenSession.create(
                userId, tokenHasher.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
    }

    private void validateRefreshTokenSession(RefreshTokenSession session, RefreshTokenClaims claims, Instant now) {
        if (!session.getUserId().equals(claims.userId()) || !session.getExpiresAt().equals(claims.expiresAt())) {
            revokeTokenFamilyAndReject(session, now, "claim_mismatch");
        }
        if (session.isRevoked()) {
            revokeTokenFamilyAndReject(session, now, "revoked_token_reuse");
        }
        if (session.isExpiredAt(now)) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void revokeTokenFamilyAndReject(RefreshTokenSession session, Instant now, String reason) {
        saveRefreshTokenSessionPort.revokeFamily(session.getFamilyId(), now);
        log.warn("refresh_token_family_revoked userId={} refreshTokenFamilyId={} reason={}",
                session.getUserId(),
                session.getFamilyId(),
                reason);
        throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void validateActiveUser(AuthUserInfo user) {
        if (!user.active()) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}
