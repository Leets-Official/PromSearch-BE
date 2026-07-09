package com.promsearch.auth.application;

public record ReissueInfo(
        String accessToken,
        String tokenType,
        Long expiresIn
) {

    public static ReissueInfo of(String accessToken, Long expiresIn) {
        return new ReissueInfo(accessToken, "Bearer", expiresIn);
    }
}
