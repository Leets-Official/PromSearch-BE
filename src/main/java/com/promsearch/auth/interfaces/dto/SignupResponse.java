package com.promsearch.auth.interfaces.dto;

import com.promsearch.user.application.SignupInfo;

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
