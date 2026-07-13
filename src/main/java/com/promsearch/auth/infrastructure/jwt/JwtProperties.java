package com.promsearch.auth.infrastructure.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotBlank(message = "auth.jwt.access-secret is required")
        String accessSecret,

        @NotBlank(message = "auth.jwt.refresh-secret is required")
        String refreshSecret,

        @NotNull(message = "auth.jwt.access-token-expiration-seconds is required")
        @Positive(message = "auth.jwt.access-token-expiration-seconds must be positive")
        Long accessTokenExpirationSeconds,

        @NotNull(message = "auth.jwt.refresh-token-expiration-seconds is required")
        @Positive(message = "auth.jwt.refresh-token-expiration-seconds must be positive")
        Long refreshTokenExpirationSeconds
) {
}
