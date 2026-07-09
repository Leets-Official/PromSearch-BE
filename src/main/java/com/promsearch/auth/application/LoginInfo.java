package com.promsearch.auth.application;

import com.promsearch.user.domain.User;

public record LoginInfo(
        String accessToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String name,
        String nickname,
        String email
) {

    public static LoginInfo of(String accessToken, Long expiresIn, User user) {
        return new LoginInfo(
                accessToken,
                "Bearer",
                expiresIn,
                user.getUserId().id(),
                user.getName(),
                user.getNickname(),
                user.getEmail()
        );
    }
}
