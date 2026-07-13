package com.promsearch.global.security;

public record AuthenticatedUserPrincipal(
        Long userId,
        String role
) {
}
