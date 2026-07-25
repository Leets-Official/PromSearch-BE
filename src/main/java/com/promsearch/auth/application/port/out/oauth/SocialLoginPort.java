package com.promsearch.auth.application.port.out.oauth;

import com.promsearch.auth.domain.enums.SocialProvider;

public interface SocialLoginPort {

    SocialProvider provider();

    SocialLoginResult exchangeCodeAndFetchUserInfo(String authorizationCode, String redirectUri);
}
