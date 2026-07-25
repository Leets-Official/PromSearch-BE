package com.promsearch.auth.application.usecase.dto;

public record AuthenticatedAccessTokenInfo(
        Long userId,
        String role
) {
}
