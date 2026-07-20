package com.promsearch.auth.application;

import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.RefreshTokenSessionRepository;
import com.promsearch.auth.application.port.out.SocialAccountRepository;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginTransactionService {

    private final SocialAccountRepository socialAccountRepository;
    private final GetUserCredentialUseCase getUserCredentialUseCase;
    private final RegisterSocialUserUseCase registerSocialUserUseCase;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final TokenHasher tokenHasher;

    @Transactional
    public LoginInfo completeLogin(SocialProvider provider, SocialUserInfo socialUserInfo) {
        Long userId = socialAccountRepository
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

        socialAccountRepository.save(SocialAccount.create(
                signupInfo.userId(), provider, socialUserInfo.providerUserId()));
        log.info("auth_social_account_linked userId={} provider={}", signupInfo.userId(), provider);

        return signupInfo.userId();
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
