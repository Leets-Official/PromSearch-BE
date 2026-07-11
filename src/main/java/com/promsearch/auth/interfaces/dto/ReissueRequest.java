package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.ReissueCommand;
import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "refresh token is required.")
        String refreshToken
) {

    public ReissueCommand toCommand() {
        return ReissueCommand.of(refreshToken);
    }
}
