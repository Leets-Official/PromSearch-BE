package com.promsearch.user.interfaces.dto;

import com.promsearch.user.application.UpdateUserProfileCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 100, message = "name must be 100 characters or less")
        String name,

        @Size(max = 100, message = "nickname must be 100 characters or less")
        String nickname,

        @Email(message = "email format is invalid")
        @Size(max = 255, message = "email must be 255 characters or less")
        String email,

        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,

        @Size(max = 500, message = "profileImageUrl must be 500 characters or less")
        String profileImageUrl
) {

    public UpdateUserProfileCommand toCommand(Long userId) {
        return UpdateUserProfileCommand.of(userId, name, nickname, email, password, profileImageUrl);
    }
}
