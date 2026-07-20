package com.promsearch.auth.application;

import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.RefreshTokenSessionRepository;
import com.promsearch.auth.application.port.out.SocialAccountRepository;
import com.promsearch.auth.application.port.out.SocialLoginClient;
import com.promsearch.auth.application.port.out.SocialLoginClient.SocialUserInfo;
import com.promsearch.auth.application.port.out.TokenHasher;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.AuthUserInfo;
import com.promsearch.user.application.GetUserCredentialUseCase;
import com.promsearch.user.application.RegisterSocialUserCommand;
import com.promsearch.user.application.RegisterSocialUserUseCase;
import com.promsearch.user.application.SignupInfo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthCommandService implements SocialLoginUseCase {

    private final List<SocialLoginClient> socialLoginClients;
    private final SocialAccountRepository socialAccountRepository;
    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final RegisterSocialUserUseCase registerSocialUserUseCase;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final TokenHasher tokenHasher;

    @Override
    @Transactional
    public LoginInfo socialLogin(SocialLoginCommand command) {
        SocialLoginClient client = resolveClient(command.provider());
        SocialUserInfo socialUserInfo = fetchSocialUserInfo(client, command);

        Long userId = socialAccountRepository
                .findByProviderAndProviderUserId(command.provider(), socialUserInfo.providerUserId())
                .map(SocialAccount::getUserId)
                .orElseGet(() -> provisionSocialUser(command.provider(), socialUserInfo));

        AuthUserInfo user = getUserCredentialUseCase.findById(userId)
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED));
        validateActiveUser(user);
        AuthenticatedUserInfo authenticatedUser = AuthenticatedUserInfo.from(user);

        RefreshToken refreshToken = refreshTokenProvider.createRefreshToken(authenticatedUser);
        saveRefreshTokenSession(authenticatedUser.userId(), refreshToken, UUID.randomUUID().toString());

        log.info("auth_social_login_succeeded userId={} provider={}", authenticatedUser.userId(), command.provider());

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

        socialAccountRepository.save(SocialAccount.create(
                signupInfo.userId(), provider, socialUserInfo.providerUserId()));
        log.info("auth_social_account_linked userId={} provider={}", signupInfo.userId(), provider);

        return signupInfo.userId();
    }

    private SocialUserInfo fetchSocialUserInfo(SocialLoginClient client, SocialLoginCommand command) {
        try {
            return client.exchangeCodeAndFetchUserInfo(command.authorizationCode(), command.redirectUri());
        } catch (AuthDomainException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("oauth_user_info_fetch_failed provider={} reason={}",
                    command.provider(), e.getClass().getSimpleName());
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private SocialLoginClient resolveClient(SocialProvider provider) {
        return socialLoginClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }

    private void saveRefreshTokenSession(Long userId, RefreshToken refreshToken, String familyId) {
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                userId, tokenHasher.hash(refreshToken.value()), familyId, refreshToken.expiresAt()));
    }

    private void validateActiveUser(AuthUserInfo user) {
        if (!user.active()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }
}
