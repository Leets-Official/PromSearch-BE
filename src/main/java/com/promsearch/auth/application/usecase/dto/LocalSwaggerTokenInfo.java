package com.promsearch.auth.application.usecase.dto;

public record LocalSwaggerTokenInfo(
        String accessToken,
        Long expiresInSeconds,
        AuthenticatedUserInfo user
) {
}
