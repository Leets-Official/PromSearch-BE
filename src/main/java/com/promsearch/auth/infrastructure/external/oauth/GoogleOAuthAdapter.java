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
public class GoogleOAuthAdapter implements SocialLoginPort {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    @Autowired
    public GoogleOAuthAdapter(GoogleOAuthProperties properties) {
        this(properties, RestClient.builder().requestFactory(OAuthRestClientFactory.create()));
    }

    GoogleOAuthAdapter(GoogleOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
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

        GoogleTokenResponse response = requireBody(restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class));

        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        return response.accessToken();
    }

    private SocialLoginResult fetchProfile(String accessToken) {
        GoogleUserResponse response = requireBody(restClient.get()
                .uri(properties.userInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserResponse.class));

        if (response.sub() == null || response.sub().isBlank()) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        if (response.email() == null || response.email().isBlank() || !Boolean.TRUE.equals(response.emailVerified())) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_EMAIL_NOT_AVAILABLE);
        }

        return new SocialLoginResult(response.sub(), response.email(), response.name(), response.picture());
    }

    private <T> T requireBody(T body) {
        if (body == null) {
            throw new AuthDomainException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        return body;
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record GoogleUserResponse(
            String sub,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            String name,
            String picture
    ) {
    }
}
