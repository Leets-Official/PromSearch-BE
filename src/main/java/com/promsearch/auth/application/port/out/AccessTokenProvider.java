package com.promsearch.auth.application.port.out;

import com.promsearch.user.application.AuthUserInfo;

public interface AccessTokenProvider {

    String createAccessToken(AuthUserInfo user);

    Long getAccessTokenExpirationSeconds();
}
