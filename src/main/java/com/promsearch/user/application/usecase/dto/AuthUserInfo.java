package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;

public record AuthUserInfo(
        Long userId,
        String email,
        String encodedPassword,
        String nickname,
        String role,
        boolean active
) {

    public static AuthUserInfo from(User user) {
        return new AuthUserInfo(
                user.getUserId().id(),
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus() == UserStatus.ACTIVE
        );
    }
}
