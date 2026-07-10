package com.promsearch.user.interfaces.dto;

import com.promsearch.user.application.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required")
        @Size(min = 8, max = 100, message = "currentPassword must be between 8 and 100 characters")
        String currentPassword,

        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword
) {

    public ChangePasswordCommand toCommand(Long userId) {
        return ChangePasswordCommand.of(userId, currentPassword, newPassword);
    }
}
