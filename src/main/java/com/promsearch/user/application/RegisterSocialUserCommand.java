package com.promsearch.user.application;

public record RegisterSocialUserCommand(
        String email,
        String nickname,
        String name,
        String profileImageUrl
) {

    public static RegisterSocialUserCommand of(String email, String nickname, String name, String profileImageUrl) {
        return new RegisterSocialUserCommand(email, nickname, name, profileImageUrl);
    }
}
