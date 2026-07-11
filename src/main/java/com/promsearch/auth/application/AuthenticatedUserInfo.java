package com.promsearch.auth.application;

import com.promsearch.user.application.AuthUserInfo;

public record AuthenticatedUserInfo(
        Long userId,
        String email,
        String role
) {

    public static AuthenticatedUserInfo from(AuthUserInfo user) {
        return new AuthenticatedUserInfo(user.userId(), user.email(), user.role());
    }
}
