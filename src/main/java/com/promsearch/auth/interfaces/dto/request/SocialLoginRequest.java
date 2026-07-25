package com.promsearch.auth.interfaces.dto.request;

import com.promsearch.auth.application.usecase.dto.SocialLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(
        @Schema(description = "OAuth 인가 코드", example = "abcd1234")
        @NotBlank(message = "인가 코드는 필수입니다.")
        String code,

        @Schema(description = "인가 코드 발급 시 사용한 리다이렉트 URI", example = "https://promsearch.com/oauth/kakao/callback")
        @NotBlank(message = "리다이렉트 URI는 필수입니다.")
        String redirectUri
) {

    public SocialLoginCommand toCommand(String provider) {
        return SocialLoginCommand.of(provider, code, redirectUri);
    }
}
