package com.promsearch.user.application.usecase.dto;

public record NicknameAvailabilityInfo(
        boolean available
) {

    public static NicknameAvailabilityInfo from(boolean exists) {
        return new NicknameAvailabilityInfo(!exists);
    }
}
