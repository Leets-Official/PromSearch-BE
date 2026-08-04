package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.crypto.HashTokenPort;
import com.promsearch.auth.application.port.out.persistence.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.token.IssueAccessTokenPort;
import com.promsearch.auth.application.port.out.token.IssueRefreshTokenPort;
import com.promsearch.auth.application.port.out.token.IssuedAccessToken;
import com.promsearch.auth.application.port.out.token.IssuedRefreshToken;
import com.promsearch.auth.application.usecase.LoginUseCase;
import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.LoginCommand;
import com.promsearch.auth.application.usecase.dto.LoginInfo;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
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
public class CredentialAuthenticationService implements LoginUseCase {

    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final PasswordEncoder passwordEncoder;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final IssueRefreshTokenPort issueRefreshTokenPort;
    private final SaveRefreshTokenSessionPort saveRefreshTokenSessionPort;
    private final HashTokenPort hashTokenPort;

    @Override
    @Transactional
    public LoginInfo login(LoginCommand command) {
        AuthUserInfo user = getUserCredentialUseCase.findByEmail(command.email())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS));

        validatePassword(command.password(), user.encodedPassword());
        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);

        IssuedRefreshToken refreshToken = issueRefreshTokenPort.issueRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, UUID.randomUUID().toString());
        IssuedAccessToken accessToken = issueAccessTokenPort.issueAccessToken(authenticatedUser);

        log.info("auth_login_succeeded userId={}", authenticatedUser.userId());

        return LoginInfo.of(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresInSeconds(),
                authenticatedUser,
                user.profileImageUrl(),
                user.nickname()
        );
    }

    private void saveRefreshTokenSession(Long userId, IssuedRefreshToken refreshToken, String familyId) {
        saveRefreshTokenSessionPort.save(RefreshTokenSession.create(
                userId, hashTokenPort.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
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
