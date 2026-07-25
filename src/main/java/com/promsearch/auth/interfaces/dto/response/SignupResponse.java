package com.promsearch.auth.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.SignupInfo;

public record SignupResponse(
        Long userId,
        String name,
        String nickname,
        String email
) {

    public static SignupResponse from(SignupInfo info) {
        return new SignupResponse(
                info.userId(),
                info.name(),
                info.nickname(),
                info.email()
        );
    }
}
