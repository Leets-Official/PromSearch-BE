package com.promsearch.auth.application.port.out;

import com.promsearch.user.domain.User;

public interface AccessTokenProvider {

    String createAccessToken(User user);

    Long getAccessTokenExpirationSeconds();
}
