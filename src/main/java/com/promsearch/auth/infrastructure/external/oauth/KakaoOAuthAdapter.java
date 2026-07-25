package com.promsearch.auth.infrastructure.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.promsearch.auth.application.port.out.oauth.SocialLoginPort;
import com.promsearch.auth.application.port.out.oauth.SocialLoginResult;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthAdapter implements SocialLoginPort {

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoOAuthAdapter(KakaoOAuthProperties properties) {
        this(properties, RestClient.builder().requestFactory(OAuthRestClientFactory.create()));
    }

    KakaoOAuthAdapter(KakaoOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialLoginResult exchangeCodeAndFetchUserInfo(String authorizationCode, String redirectUri) {
        return OAuthExceptionTranslator.execute(provider(), () -> {
            String accessToken = exchangeAccessToken(authorizationCode, redirectUri);
            return fetchProfile(accessToken);
        });
    }

    private String exchangeAccessToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            form.add("client_secret", properties.clientSecret());
        }

        KakaoTokenResponse response = requireBody(restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class));

        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        return response.accessToken();
    }

    private SocialLoginResult fetchProfile(String accessToken) {
        KakaoUserResponse response = requireBody(restClient.get()
                .uri(properties.userInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class));

        if (response.id() == null) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }

        KakaoUserResponse.KakaoAccount account = response.kakaoAccount();
        KakaoUserResponse.KakaoAccount.KakaoProfile profile = account != null ? account.profile() : null;
        String email = account != null ? account.email() : null;
        boolean emailVerified = account != null
                && Boolean.TRUE.equals(account.isEmailValid())
                && Boolean.TRUE.equals(account.isEmailVerified());

        if (email == null || email.isBlank() || !emailVerified) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_EMAIL_NOT_AVAILABLE);
        }

        return new SocialLoginResult(
                String.valueOf(response.id()),
                email,
                profile != null ? profile.nickname() : null,
                profile != null ? profile.profileImageUrl() : null
        );
    }

    private <T> T requireBody(T body) {
        if (body == null) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        return body;
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {

        private record KakaoAccount(
                String email,
                @JsonProperty("is_email_valid") Boolean isEmailValid,
                @JsonProperty("is_email_verified") Boolean isEmailVerified,
                KakaoProfile profile
        ) {

            private record KakaoProfile(
                    String nickname,
                    @JsonProperty("profile_image_url") String profileImageUrl
            ) {
            }
        }
    }
}
