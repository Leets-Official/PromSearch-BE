package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.crypto.HashTokenPort;
import com.promsearch.auth.application.port.out.oauth.SocialLoginResult;
import com.promsearch.auth.application.port.out.persistence.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.persistence.social.LoadSocialAccountPort;
import com.promsearch.auth.application.port.out.persistence.social.SaveSocialAccountPort;
import com.promsearch.auth.application.port.out.token.IssueAccessTokenPort;
import com.promsearch.auth.application.port.out.token.IssueRefreshTokenPort;
import com.promsearch.auth.application.port.out.token.IssuedAccessToken;
import com.promsearch.auth.application.port.out.token.IssuedRefreshToken;
import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.LoginInfo;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.RegisterSocialUserUseCase;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import com.promsearch.user.application.usecase.dto.RegisterSocialUserCommand;
import com.promsearch.user.application.usecase.dto.SignupInfo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginCompletionService {

    private final LoadSocialAccountPort loadSocialAccountPort;
    private final SaveSocialAccountPort saveSocialAccountPort;
    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final RegisterSocialUserUseCase registerSocialUserUseCase;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final IssueRefreshTokenPort issueRefreshTokenPort;
    private final SaveRefreshTokenSessionPort saveRefreshTokenSessionPort;
    private final HashTokenPort hashTokenPort;

    @Transactional
    public LoginInfo completeLogin(SocialProvider provider, SocialLoginResult socialLoginResult) {
        Long userId = loadSocialAccountPort
                .findByProviderAndProviderUserId(provider, socialLoginResult.providerUserId())
                .map(SocialAccount::getUserId)
                .orElseGet(() -> provisionSocialUser(provider, socialLoginResult));

        AuthUserInfo user = getUserCredentialUseCase.findById(userId)
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED));
        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);

        IssuedRefreshToken refreshToken = issueRefreshTokenPort.issueRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, UUID.randomUUID().toString());
        IssuedAccessToken accessToken = issueAccessTokenPort.issueAccessToken(authenticatedUser);

        return LoginInfo.of(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresInSeconds(),
                authenticatedUser,
                user.name(),
                user.nickname()
        );
    }

    private Long provisionSocialUser(SocialProvider provider, SocialLoginResult socialLoginResult) {
        SignupInfo signupInfo = registerSocialUserUseCase.registerSocialUser(RegisterSocialUserCommand.of(
                socialLoginResult.email(),
                socialLoginResult.nickname(),
                socialLoginResult.nickname(),
                socialLoginResult.profileImageUrl()
        ));

        saveSocialAccountPort.save(SocialAccount.create(
                signupInfo.userId(), provider, socialLoginResult.providerUserId()));
        log.info("auth_social_account_linked userId={} provider={}", signupInfo.userId(), provider);

        return signupInfo.userId();
    }

    private void saveRefreshTokenSession(Long userId, IssuedRefreshToken refreshToken, String familyId) {
        saveRefreshTokenSessionPort.save(RefreshTokenSession.create(
                userId, hashTokenPort.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
    }

    private void validateActiveUser(AuthUserInfo user) {
        if (!user.active()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }
}
