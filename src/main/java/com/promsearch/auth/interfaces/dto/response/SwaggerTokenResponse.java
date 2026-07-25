package com.promsearch.auth.interfaces.dto.response;

import com.promsearch.auth.application.usecase.dto.LocalSwaggerTokenInfo;

public record SwaggerTokenResponse(
        String accessToken,
        String tokenType,
        String authorizationHeader,
        Long expiresIn,
        Long userId,
        String email,
        String role
) {

    public static SwaggerTokenResponse from(LocalSwaggerTokenInfo tokenInfo) {
        String tokenType = "Bearer";
        return new SwaggerTokenResponse(
                tokenInfo.accessToken(),
                tokenType,
                tokenType + " " + tokenInfo.accessToken(),
                tokenInfo.expiresInSeconds(),
                tokenInfo.user().userId(),
                tokenInfo.user().email(),
                tokenInfo.user().role()
        );
    }
}
