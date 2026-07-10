package com.promsearch.auth.application;

import com.promsearch.user.application.AuthUserInfo;

public record LoginInfo(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String name,
        String nickname,
        String email
) {

    public static LoginInfo of(String accessToken, Long expiresIn, AuthUserInfo user) {
        return of(accessToken, null, expiresIn, user);
    }

    public static LoginInfo of(String accessToken, String refreshToken, Long expiresIn, AuthUserInfo user) {
        return new LoginInfo(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                user.userId(),
                user.name(),
                user.nickname(),
                user.email()
        );
    }
}
