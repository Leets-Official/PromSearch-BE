package com.promsearch.auth.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.auth.application.port.out.crypto.HashTokenPort;
import com.promsearch.auth.application.port.out.oauth.SocialLoginPort;
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
import com.promsearch.auth.application.usecase.dto.SocialLoginCommand;
import com.promsearch.auth.domain.RefreshTokenSession;
import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import com.promsearch.user.application.usecase.GetUserCredentialUseCase;
import com.promsearch.user.application.usecase.dto.RegisterSocialUserCommand;
import com.promsearch.user.application.usecase.RegisterSocialUserUseCase;
import com.promsearch.user.application.usecase.dto.SignupInfo;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocialAuthenticationServiceTest {

    private FakeSocialLoginPort kakaoPort;
    private FakeSocialAccountRepository socialAccountRepository;
    private FakeUserDirectory userDirectory;
    private SocialAuthenticationService socialAuthenticationService;

    @BeforeEach
    void setUp() {
        kakaoPort = new FakeSocialLoginPort(SocialProvider.KAKAO);
        socialAccountRepository = new FakeSocialAccountRepository();
        userDirectory = new FakeUserDirectory();
        SocialLoginCompletionService socialLoginCompletionService = new SocialLoginCompletionService(
                socialAccountRepository,
                socialAccountRepository,
                userDirectory,
                userDirectory,
                new FakeIssueAccessTokenPort(),
                new FakeIssueRefreshTokenPort(),
                new FakeRefreshTokenSessionRepository(),
                new FakeHashTokenPort()
        );
        socialAuthenticationService = new SocialAuthenticationService(List.of(kakaoPort), socialLoginCompletionService);
    }

    @Test
    void socialLoginCreatesNewUserAndLinksSocialAccountOnFirstLogin() {
        kakaoPort.willReturn(new SocialLoginResult(
                "kakao-1", "new@example.com", "닉네임", "https://image.test/a.png"));

        LoginInfo loginInfo = socialAuthenticationService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback"));

        assertThat(loginInfo.email()).isEqualTo("new@example.com");
        assertThat(loginInfo.profileImageUrl()).isEqualTo("https://image.test/a.png");
        assertThat(loginInfo.accessToken()).isNotBlank();
        assertThat(loginInfo.refreshToken()).isNotBlank();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
                .isPresent()
                .get()
                .extracting(SocialAccount::getUserId)
                .isEqualTo(loginInfo.userId());
    }

    @Test
    void socialLoginReusesExistingLinkedAccountWithoutCreatingNewUser() {
        Long existingUserId = userDirectory.seed("existing@example.com", "기존유저", true);
        socialAccountRepository.save(SocialAccount.create(existingUserId, SocialProvider.KAKAO, "kakao-1"));
        kakaoPort.willReturn(new SocialLoginResult("kakao-1", "existing@example.com", "기존유저", null));

        LoginInfo loginInfo = socialAuthenticationService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback"));

        assertThat(loginInfo.userId()).isEqualTo(existingUserId);
        assertThat(userDirectory.registeredCount()).isEqualTo(1);
    }

    @Test
    void socialLoginRejectsInactiveUser() {
        Long inactiveUserId = userDirectory.seed("inactive@example.com", "휴면유저", false);
        socialAccountRepository.save(SocialAccount.create(inactiveUserId, SocialProvider.KAKAO, "kakao-1"));
        kakaoPort.willReturn(new SocialLoginResult("kakao-1", "inactive@example.com", "휴면유저", null));

        assertThatThrownBy(() -> socialAuthenticationService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback")))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    @Test
    void socialLoginFailsForUnsupportedProvider() {
        assertThatThrownBy(() -> socialAuthenticationService.socialLogin(
                SocialLoginCommand.of("GOOGLE", "auth-code", "https://promsearch.com/callback")))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    @Test
    void socialLoginPropagatesClientExceptionWithoutTranslation() {
        RuntimeException clientFailure = new RuntimeException("kakao server error");
        kakaoPort.willThrow(clientFailure);

        assertThatThrownBy(() -> socialAuthenticationService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback")))
                .isSameAs(clientFailure);
    }

    private static class FakeSocialLoginPort implements SocialLoginPort {

        private final SocialProvider provider;
        private SocialLoginResult result;
        private RuntimeException failure;

        private FakeSocialLoginPort(SocialProvider provider) {
            this.provider = provider;
        }

        void willReturn(SocialLoginResult result) {
            this.result = result;
        }

        void willThrow(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public SocialProvider provider() {
            return provider;
        }

        @Override
        public SocialLoginResult exchangeCodeAndFetchUserInfo(String authorizationCode, String redirectUri) {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private static class FakeSocialAccountRepository implements LoadSocialAccountPort, SaveSocialAccountPort {

        private final Map<String, SocialAccount> accountsByKey = new HashMap<>();
        private long nextId = 1L;

        @Override
        public SocialAccount save(SocialAccount socialAccount) {
            String key = key(socialAccount.getProvider(), socialAccount.getProviderUserId());
            if (accountsByKey.containsKey(key)) {
                throw new AuthDomainException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
            SocialAccount saved = SocialAccount.reconstruct(
                    nextId++, socialAccount.getUserId(), socialAccount.getProvider(), socialAccount.getProviderUserId());
            accountsByKey.put(key, saved);
            return saved;
        }

        @Override
        public Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId) {
            return Optional.ofNullable(accountsByKey.get(key(provider, providerUserId)));
        }

        private String key(SocialProvider provider, String providerUserId) {
            return provider + ":" + providerUserId;
        }
    }

    private static class FakeUserDirectory implements GetUserCredentialUseCase, RegisterSocialUserUseCase {

        private final Map<Long, AuthUserInfo> usersById = new HashMap<>();
        private long nextId = 1L;

        Long seed(String email, String nickname, boolean active) {
            Long userId = nextId++;
            usersById.put(userId, new AuthUserInfo(
                    userId, email, "encoded-placeholder", nickname, null, "USER", active));
            return userId;
        }

        int registeredCount() {
            return usersById.size();
        }

        @Override
        public Optional<AuthUserInfo> findByEmail(String email) {
            return usersById.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public Optional<AuthUserInfo> findById(Long userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public SignupInfo registerSocialUser(RegisterSocialUserCommand command) {
            if (findByEmail(command.email()).isPresent()) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
            }
            Long userId = nextId++;
            usersById.put(userId, new AuthUserInfo(
                    userId,
                    command.email(),
                    "encoded-placeholder",
                    command.nickname(),
                    command.profileImageUrl(),
                    "USER",
                    true
            ));
            return new SignupInfo(userId, command.nickname(), command.email());
        }
    }

    private static class FakeIssueAccessTokenPort implements IssueAccessTokenPort {

        @Override
        public IssuedAccessToken issueAccessToken(AuthenticatedUserInfo user) {
            return new IssuedAccessToken("access-token-for-" + user.userId(), 3600L);
        }
    }

    private static class FakeIssueRefreshTokenPort implements IssueRefreshTokenPort {

        @Override
        public IssuedRefreshToken issueRefreshToken(AuthenticatedUserInfo user) {
            return new IssuedRefreshToken(
                    "refresh-token-for-" + user.userId(),
                    Instant.now().plusSeconds(1_209_600)
            );
        }
    }

    private static class FakeRefreshTokenSessionRepository implements SaveRefreshTokenSessionPort {

        @Override
        public RefreshTokenSession save(RefreshTokenSession session) {
            return session;
        }

        @Override
        public void revokeFamily(String familyId, Instant revokedAt) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeHashTokenPort implements HashTokenPort {

        @Override
        public String hash(String token) {
            return "hashed:" + token;
        }
    }
}
