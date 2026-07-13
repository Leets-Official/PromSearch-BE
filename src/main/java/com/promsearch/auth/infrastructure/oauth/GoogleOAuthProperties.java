package com.promsearch.auth.infrastructure.oauth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "auth.oauth.google")
@Validated
public record GoogleOAuthProperties(
        @NotBlank String clientId,
        String clientSecret,
        @NotBlank String tokenUri,
        @NotBlank String userInfoUri
) {
}
