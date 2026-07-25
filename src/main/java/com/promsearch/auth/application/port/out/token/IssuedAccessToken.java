package com.promsearch.auth.application.port.out.token;

public record IssuedAccessToken(
        String value,
        Long expiresInSeconds
) {
}
