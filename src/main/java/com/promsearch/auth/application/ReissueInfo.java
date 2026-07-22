package com.promsearch.auth.application;

public record ReissueInfo(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {

    public static ReissueInfo of(String accessToken, String refreshToken, Long expiresIn) {
        return new ReissueInfo(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
