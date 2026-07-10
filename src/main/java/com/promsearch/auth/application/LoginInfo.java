package com.promsearch.auth.application;

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

    public static LoginInfo of(
            String accessToken,
            Long expiresIn,
            AuthenticatedUserInfo user,
            String name,
            String nickname
    ) {
        return of(accessToken, null, expiresIn, user, name, nickname);
    }

    public static LoginInfo of(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            AuthenticatedUserInfo user,
            String name,
            String nickname
    ) {
        return new LoginInfo(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                user.userId(),
                name,
                nickname,
                user.email()
        );
    }
}
