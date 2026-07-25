package com.promsearch.auth.application.port.out.oauth;

public record SocialLoginResult(
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
