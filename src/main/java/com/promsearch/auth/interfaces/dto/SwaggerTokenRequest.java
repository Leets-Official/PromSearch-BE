package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.AuthenticatedUserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "Swagger local 테스트 토큰 발급 요청")
public record SwaggerTokenRequest(
        @Schema(description = "토큰 subject로 사용할 사용자 ID. 비우면 1로 발급합니다.", example = "1")
        @Positive(message = "userId must be positive")
        Long userId,

        @Schema(description = "토큰 claim에 넣을 이메일. 비우면 swagger-local@example.com을 사용합니다.", example = "swagger-local@example.com")
        @Email(message = "email format is invalid")
        String email,

        @Schema(description = "토큰 claim에 넣을 역할", example = "USER", allowableValues = {"USER", "ADMIN"})
        @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN")
        String role
) {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_EMAIL = "swagger-local@example.com";
    private static final String DEFAULT_ROLE = "USER";

    public static AuthenticatedUserInfo toAuthenticatedUser(SwaggerTokenRequest request) {
        if (request == null) {
            return new AuthenticatedUserInfo(DEFAULT_USER_ID, DEFAULT_EMAIL, DEFAULT_ROLE);
        }
        return new AuthenticatedUserInfo(
                request.userId() == null ? DEFAULT_USER_ID : request.userId(),
                isBlank(request.email()) ? DEFAULT_EMAIL : request.email(),
                isBlank(request.role()) ? DEFAULT_ROLE : request.role()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
