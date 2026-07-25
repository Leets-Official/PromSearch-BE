package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.refresh.SaveRefreshTokenSessionPort;
import com.promsearch.auth.application.port.out.social.LoadSocialAccountPort;
import com.promsearch.auth.application.port.out.social.SaveSocialAccountPort;
import com.promsearch.auth.application.port.out.social.SocialLoginClient.SocialUserInfo;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.token.TokenHasher;
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
public class SocialLoginTransactionService {

    private final LoadSocialAccountPort loadSocialAccountPort;
    private final SaveSocialAccountPort saveSocialAccountPort;
    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final RegisterSocialUserUseCase registerSocialUserUseCase;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final SaveRefreshTokenSessionPort saveRefreshTokenSessionPort;
    private final TokenHasher tokenHasher;

    @Transactional
    public LoginInfo completeLogin(SocialProvider provider, SocialUserInfo socialUserInfo) {
        Long userId = loadSocialAccountPort
                .findByProviderAndProviderUserId(provider, socialUserInfo.providerUserId())
                .map(SocialAccount::getUserId)
                .orElseGet(() -> provisionSocialUser(provider, socialUserInfo));

        AuthUserInfo user = getUserCredentialUseCase.findById(userId)
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED));
        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);

        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, UUID.randomUUID().toString());

        return LoginInfo.of(
                accessTokenProvider.createAccessToken(authenticatedUser),
                refreshToken.value(),
                accessTokenProvider.getAccessTokenExpirationSeconds(),
                authenticatedUser,
                user.name(),
                user.nickname()
        );
    }

    private Long provisionSocialUser(SocialProvider provider, SocialUserInfo socialUserInfo) {
        SignupInfo signupInfo = registerSocialUserUseCase.registerSocialUser(RegisterSocialUserCommand.of(
                socialUserInfo.email(),
                socialUserInfo.nickname(),
                socialUserInfo.nickname(),
                socialUserInfo.profileImageUrl()
        ));

        saveSocialAccountPort.save(SocialAccount.create(
                signupInfo.userId(), provider, socialUserInfo.providerUserId()));
        log.info("auth_social_account_linked userId={} provider={}", signupInfo.userId(), provider);

        return signupInfo.userId();
    }

    private void saveRefreshTokenSession(Long userId, RefreshToken refreshToken, String familyId) {
        saveRefreshTokenSessionPort.save(RefreshTokenSession.create(
                userId, tokenHasher.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
    }

    private void validateActiveUser(AuthUserInfo user) {
        if (!user.active()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }
}
