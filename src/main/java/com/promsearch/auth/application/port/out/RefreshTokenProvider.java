package com.promsearch.auth.application.port.out;

import com.promsearch.user.domain.User;

public interface RefreshTokenProvider {

    String createRefreshToken(User user);

    Long getUserId(String refreshToken);
}
