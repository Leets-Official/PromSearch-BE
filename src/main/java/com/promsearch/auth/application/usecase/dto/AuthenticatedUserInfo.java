package com.promsearch.auth.application.usecase.dto;

import com.promsearch.user.application.usecase.dto.AuthUserInfo;

public record AuthenticatedUserInfo(
        Long userId,
        String email,
        String role
) {

    public static AuthenticatedUserInfo from(AuthUserInfo user) {
        return new AuthenticatedUserInfo(user.userId(), user.email(), user.role());
    }
}
