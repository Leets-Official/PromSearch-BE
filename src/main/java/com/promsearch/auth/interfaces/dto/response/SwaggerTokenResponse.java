package com.promsearch.auth.interfaces.dto.response;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;

public record SwaggerTokenResponse(
        String accessToken,
        String tokenType,
        String authorizationHeader,
        Long expiresIn,
        Long userId,
        String email,
        String role
) {

    public static SwaggerTokenResponse of(String accessToken, Long expiresIn, AuthenticatedUserInfo user) {
        String tokenType = "Bearer";
        return new SwaggerTokenResponse(
                accessToken,
                tokenType,
                tokenType + " " + accessToken,
                expiresIn,
                user.userId(),
                user.email(),
                user.role()
        );
    }
}
