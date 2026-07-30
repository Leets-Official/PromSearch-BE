package com.promsearch.auth.application.usecase.dto;

public record LoginInfo(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String profileImageUrl,
        String nickname,
        String email
) {

    public static LoginInfo of(
            String accessToken,
            Long expiresIn,
            AuthenticatedUserInfo user,
            String profileImageUrl,
            String nickname
    ) {
        return of(accessToken, null, expiresIn, user, profileImageUrl, nickname);
    }

    public static LoginInfo of(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            AuthenticatedUserInfo user,
            String profileImageUrl,
            String nickname
    ) {
        return new LoginInfo(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                user.userId(),
                profileImageUrl,
                nickname,
                user.email()
        );
    }
}
