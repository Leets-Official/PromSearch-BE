package com.promsearch.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.promsearch.auth.application.port.out.social.SocialLoginClient.SocialUserInfo;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoSocialLoginClientTest {

    private static final KakaoOAuthProperties PROPERTIES = new KakaoOAuthProperties(
            "test-client-id",
            "test-client-secret",
            "https://kauth.kakao.com/oauth/token",
            "https://kapi.kakao.com/v2/user/me"
    );

    @Test
    void exchangeCodeAndFetchUserInfoSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"kakao-access-token\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":123456,\"kakao_account\":{\"email\":\"kakao-user@example.com\","
                                + "\"is_email_valid\":true,\"is_email_verified\":true,"
                                + "\"profile\":{\"nickname\":\"카카오유저\","
                                + "\"profile_image_url\":\"https://example.com/profile.png\"}}}",
                        MediaType.APPLICATION_JSON));

        SocialUserInfo userInfo = client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback");

        assertThat(client.provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(userInfo.providerUserId()).isEqualTo("123456");
        assertThat(userInfo.email()).isEqualTo("kakao-user@example.com");
        assertThat(userInfo.nickname()).isEqualTo("카카오유저");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");

        server.verify();
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenEmailNotConsented() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andRespond(withSuccess(
                        "{\"id\":123456,\"kakao_account\":{\"profile\":{\"nickname\":\"카카오유저\"}}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("소셜 계정에서 이메일 정보를 가져올 수 없습니다.");
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenEmailNotVerified() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andRespond(withSuccess(
                        "{\"id\":123456,\"kakao_account\":{\"email\":\"kakao-user@example.com\","
                                + "\"is_email_valid\":true,\"is_email_verified\":false,"
                                + "\"profile\":{\"nickname\":\"카카오유저\"}}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("소셜 계정에서 이메일 정보를 가져올 수 없습니다.");
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenTokenResponseMissingAccessToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void exchangeCodeAndFetchUserInfoTranslatesRateLimitResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_RATE_LIMITED);
    }

    @Test
    void exchangeCodeAndFetchUserInfoTranslatesUnauthorizedTokenResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    @Test
    void exchangeCodeAndFetchUserInfoTranslatesProviderServerError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoSocialLoginClient client = new KakaoSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
    }
}
