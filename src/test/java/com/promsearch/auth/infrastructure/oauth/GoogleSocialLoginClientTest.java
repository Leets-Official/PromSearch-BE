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

class GoogleSocialLoginClientTest {

    private static final GoogleOAuthProperties PROPERTIES = new GoogleOAuthProperties(
            "test-client-id",
            "test-client-secret",
            "https://oauth2.googleapis.com/token",
            "https://www.googleapis.com/oauth2/v3/userinfo"
    );

    @Test
    void exchangeCodeAndFetchUserInfoSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"google-access-token\",\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"sub\":\"google-987\",\"email\":\"google-user@example.com\",\"email_verified\":true,"
                                + "\"name\":\"구글유저\",\"picture\":\"https://example.com/profile.png\"}",
                        MediaType.APPLICATION_JSON));

        SocialUserInfo userInfo = client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback");

        assertThat(client.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(userInfo.providerUserId()).isEqualTo("google-987");
        assertThat(userInfo.email()).isEqualTo("google-user@example.com");
        assertThat(userInfo.nickname()).isEqualTo("구글유저");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");

        server.verify();
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenEmailMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andRespond(withSuccess("{\"sub\":\"google-987\",\"name\":\"구글유저\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("소셜 계정에서 이메일 정보를 가져올 수 없습니다.");
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenEmailNotVerified() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(PROPERTIES.userInfoUri()))
                .andRespond(withSuccess(
                        "{\"sub\":\"google-987\",\"email\":\"google-user@example.com\",\"email_verified\":false,"
                                + "\"name\":\"구글유저\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("소셜 계정에서 이메일 정보를 가져올 수 없습니다.");
    }

    @Test
    void exchangeCodeAndFetchUserInfoFailsWhenTokenResponseMissingAccessToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void exchangeCodeAndFetchUserInfoTranslatesRateLimitResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

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
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

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
        GoogleSocialLoginClient client = new GoogleSocialLoginClient(PROPERTIES, builder);

        server.expect(requestTo(PROPERTIES.tokenUri()))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
    }
}
