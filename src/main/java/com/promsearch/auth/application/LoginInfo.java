package com.promsearch.auth.application;

import com.promsearch.user.domain.User;

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

    public static LoginInfo of(String accessToken, Long expiresIn, User user) {
        return of(accessToken, null, expiresIn, user);
    }

    public static LoginInfo of(String accessToken, String refreshToken, Long expiresIn, User user) {
        return new LoginInfo(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                user.getUserId().id(),
                user.getName(),
                user.getNickname(),
                user.getEmail()
        );
    }
}
