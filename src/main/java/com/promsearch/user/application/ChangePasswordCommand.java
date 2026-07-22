package com.promsearch.user.application;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {

    public static ChangePasswordCommand of(Long userId, String currentPassword, String newPassword) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }
}
