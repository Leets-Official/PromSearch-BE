package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.LoginInfo;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String name,
        String nickname,
        String email
) {

    public static LoginResponse from(LoginInfo info) {
        return new LoginResponse(
                info.accessToken(),
                info.refreshToken(),
                info.tokenType(),
                info.expiresIn(),
                info.userId(),
                info.name(),
                info.nickname(),
                info.email()
        );
    }
}
