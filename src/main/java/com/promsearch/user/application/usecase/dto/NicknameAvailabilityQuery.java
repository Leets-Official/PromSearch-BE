package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;

public record NicknameAvailabilityQuery(
        String nickname
) {

    private static final int NICKNAME_MAX_LENGTH = 100;

    public NicknameAvailabilityQuery {
        if (nickname == null || nickname.isBlank() || nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new UserDomainException(UserErrorCode.INVALID_NICKNAME);
        }
    }

    public static NicknameAvailabilityQuery of(String nickname) {
        return new NicknameAvailabilityQuery(nickname);
    }
}
