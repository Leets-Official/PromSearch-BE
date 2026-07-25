package com.promsearch.auth.application.usecase.dto;

import com.promsearch.auth.domain.enums.SocialProvider;

public record SocialLoginCommand(
        SocialProvider provider,
        String authorizationCode,
        String redirectUri
) {

    public static SocialLoginCommand of(String provider, String authorizationCode, String redirectUri) {
        return new SocialLoginCommand(SocialProvider.from(provider), authorizationCode, redirectUri);
    }
}
