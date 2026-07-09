package com.promsearch.auth.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        String secret,
        Long accessTokenExpirationSeconds
) {
}
