package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.ReissueCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record ReissueRequest(
        @Schema(description = "로그인 또는 재발급 응답으로 받은 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "refresh token is required.")
        String refreshToken
) {

    public ReissueCommand toCommand() {
        return ReissueCommand.of(refreshToken);
    }
}
