package com.promsearch.auth.infrastructure.external.oauth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "auth.oauth.kakao")
@Validated
public record KakaoOAuthProperties(
        @NotBlank String clientId,
        String clientSecret,
        @NotBlank String tokenUri,
        @NotBlank String userInfoUri
) {
}
