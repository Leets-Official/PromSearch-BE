package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.AuthenticatedUserInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SwaggerTokenRequest(
        @Positive(message = "userId must be positive")
        Long userId,

        @Email(message = "email format is invalid")
        String email,

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
