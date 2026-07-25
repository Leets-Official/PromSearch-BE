package com.promsearch.auth.application.port.out.social;

import com.promsearch.auth.domain.enums.SocialProvider;

public interface SocialLoginClient {

    SocialProvider provider();

    SocialUserInfo exchangeCodeAndFetchUserInfo(String authorizationCode, String redirectUri);

    record SocialUserInfo(
            String providerUserId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
    }
}
