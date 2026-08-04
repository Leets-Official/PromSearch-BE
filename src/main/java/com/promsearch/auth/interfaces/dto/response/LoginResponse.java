package com.promsearch.auth.interfaces.dto.response;

import com.promsearch.auth.application.usecase.dto.LoginInfo;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String profileImageUrl,
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
                info.profileImageUrl(),
                info.nickname(),
                info.email()
        );
    }
}
