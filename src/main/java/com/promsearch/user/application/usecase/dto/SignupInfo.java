package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.User;

public record SignupInfo(
        Long userId,
        String nickname,
        String email
) {

    public static SignupInfo from(User user) {
        return new SignupInfo(
                user.getUserId().id(),
                user.getNickname(),
                user.getEmail()
        );
    }
}
