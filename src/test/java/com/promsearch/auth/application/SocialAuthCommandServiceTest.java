package com.promsearch.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocialAuthCommandServiceTest {

    private FakeSocialLoginClient kakaoClient;
    private FakeSocialAccountRepository socialAccountRepository;
    private FakeUserDirectory userDirectory;
    private SocialAuthCommandService socialAuthCommandService;

    @BeforeEach
    void setUp() {
        kakaoClient = new FakeSocialLoginClient(SocialProvider.KAKAO);
        socialAccountRepository = new FakeSocialAccountRepository();
        userDirectory = new FakeUserDirectory();
        socialAuthCommandService = new SocialAuthCommandService(
                List.of(kakaoClient),
                socialAccountRepository,
                userDirectory,
                userDirectory,
                new FakeAccessTokenProvider(),
                new FakeRefreshTokenProvider(),
                new FakeRefreshTokenSessionRepository(),
                new FakeTokenHasher()
        );
    }

    @Test
    void socialLoginCreatesNewUserAndLinksSocialAccountOnFirstLogin() {
        kakaoClient.willReturn(new SocialUserInfo("kakao-1", "new@example.com", "닉네임", "https://image.test/a.png"));

        LoginInfo loginInfo = socialAuthCommandService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback"));

        assertThat(loginInfo.email()).isEqualTo("new@example.com");
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
        kakaoClient.willReturn(new SocialUserInfo("kakao-1", "existing@example.com", "기존유저", null));

        LoginInfo loginInfo = socialAuthCommandService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback"));

        assertThat(loginInfo.userId()).isEqualTo(existingUserId);
        assertThat(userDirectory.registeredCount()).isEqualTo(1);
    }

    @Test
    void socialLoginRejectsInactiveUser() {
        Long inactiveUserId = userDirectory.seed("inactive@example.com", "휴면유저", false);
        socialAccountRepository.save(SocialAccount.create(inactiveUserId, SocialProvider.KAKAO, "kakao-1"));
        kakaoClient.willReturn(new SocialUserInfo("kakao-1", "inactive@example.com", "휴면유저", null));

        assertThatThrownBy(() -> socialAuthCommandService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback")))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    @Test
    void socialLoginFailsForUnsupportedProvider() {
        assertThatThrownBy(() -> socialAuthCommandService.socialLogin(
                SocialLoginCommand.of("GOOGLE", "auth-code", "https://promsearch.com/callback")))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    @Test
    void socialLoginWrapsClientRuntimeExceptionAsAuthenticationFailure() {
        kakaoClient.willThrow(new RuntimeException("kakao server error"));

        assertThatThrownBy(() -> socialAuthCommandService.socialLogin(
                SocialLoginCommand.of("KAKAO", "auth-code", "https://promsearch.com/callback")))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    private static class FakeSocialLoginClient implements SocialLoginClient {

        private final SocialProvider provider;
        private SocialUserInfo userInfo;
        private RuntimeException failure;

        private FakeSocialLoginClient(SocialProvider provider) {
            this.provider = provider;
        }

        void willReturn(SocialUserInfo userInfo) {
            this.userInfo = userInfo;
        }

        void willThrow(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public SocialProvider provider() {
            return provider;
        }

        @Override
        public SocialUserInfo exchangeCodeAndFetchUserInfo(String authorizationCode, String redirectUri) {
            if (failure != null) {
                throw failure;
            }
            return userInfo;
        }
    }

    private static class FakeSocialAccountRepository implements SocialAccountRepository {

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
            usersById.put(userId, new AuthUserInfo(userId, email, "encoded-placeholder", nickname, nickname, "USER", active));
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
                    userId, command.email(), "encoded-placeholder", command.nickname(), command.name(), "USER", true));
            return new SignupInfo(userId, command.name(), command.nickname(), command.email());
        }
    }

    private static class FakeAccessTokenProvider implements AccessTokenProvider {

        @Override
        public String createAccessToken(AuthenticatedUserInfo user) {
            return "access-token-for-" + user.userId();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long getAccessTokenExpirationSeconds() {
            return 3600L;
        }
    }

    private static class FakeRefreshTokenProvider implements RefreshTokenProvider {

        @Override
        public RefreshToken createRefreshToken(AuthenticatedUserInfo user) {
            return new RefreshToken("refresh-token-for-" + user.userId(), Instant.now().plusSeconds(1_209_600));
        }

        @Override
        public RefreshTokenClaims parse(String refreshToken) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeRefreshTokenSessionRepository implements RefreshTokenSessionRepository {

        @Override
        public RefreshTokenSession save(RefreshTokenSession session) {
            return session;
        }

        @Override
        public Optional<RefreshTokenSession> findByTokenHashForUpdate(String tokenHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revokeFamily(String familyId, Instant revokedAt) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeTokenHasher implements TokenHasher {

        @Override
        public String hash(String token) {
            return "hashed:" + token;
        }
    }
}
