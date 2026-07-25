package com.promsearch.auth.application.port.out.token;

public record AccessTokenClaims(
        Long userId,
        String role
) {
}
