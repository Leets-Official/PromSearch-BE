package com.promsearch.user.application;

public record UpdateUserProfileCommand(
        Long userId,
        String name,
        String nickname,
        String email,
        String password,
        String profileImageUrl
) {

    public static UpdateUserProfileCommand of(
            Long userId,
            String name,
            String nickname,
            String email,
            String password,
            String profileImageUrl
    ) {
        return new UpdateUserProfileCommand(userId, name, nickname, email, password, profileImageUrl);
    }
}
