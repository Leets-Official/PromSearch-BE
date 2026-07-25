package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.crypto.HashTokenPort;
import com.promsearch.auth.application.port.out.persistence.refresh.LoadRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.persistence.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.token.IssueAccessTokenPort;
import com.promsearch.auth.application.port.out.token.IssueRefreshTokenPort;
import com.promsearch.auth.application.port.out.token.IssuedAccessToken;
import com.promsearch.auth.application.port.out.token.IssuedRefreshToken;
import com.promsearch.auth.application.port.out.token.RefreshTokenClaims;
import com.promsearch.auth.application.port.out.token.VerifyRefreshTokenPort;
import com.promsearch.auth.application.usecase.ReissueUseCase;
import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.ReissueCommand;
import com.promsearch.auth.application.usecase.dto.ReissueInfo;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenReissueService implements ReissueUseCase {

    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final VerifyRefreshTokenPort verifyRefreshTokenPort;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final IssueRefreshTokenPort issueRefreshTokenPort;
    private final LoadRefreshTokenSessionPort loadRefreshTokenSessionPort;
    private final SaveRefreshTokenSessionPort saveRefreshTokenSessionPort;
    private final HashTokenPort hashTokenPort;

    @Override
    @Transactional
    public ReissueInfo reissue(ReissueCommand command) {
        RefreshTokenClaims claims = verifyRefreshTokenPort.verifyRefreshToken(command.refreshToken());
        RefreshTokenSession session = loadRefreshTokenSessionPort
                .findByTokenHashForUpdate(hashTokenPort.hash(command.refreshToken()))
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));
        Instant now = Instant.now();
        validateRefreshTokenSession(session, claims, now);

        AuthUserInfo user = getUserCredentialUseCase.findById(claims.userId())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));

        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);
        session.revoke(now);
        saveRefreshTokenSessionPort.save(session);
        IssuedRefreshToken refreshToken = issueRefreshTokenPort.issueRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, session.getFamilyId());
        IssuedAccessToken accessToken = issueAccessTokenPort.issueAccessToken(authenticatedUser);

        log.info("auth_token_reissued userId={} refreshTokenFamilyId={}",
                authenticatedUser.userId(),
                session.getFamilyId());

        return ReissueInfo.of(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresInSeconds()
        );
    }

    private void saveRefreshTokenSession(Long userId, IssuedRefreshToken refreshToken, String familyId) {
        saveRefreshTokenSessionPort.save(RefreshTokenSession.create(
                userId, hashTokenPort.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
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

    private void validateActiveUser(AuthUserInfo user) {
        if (!user.active()) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
