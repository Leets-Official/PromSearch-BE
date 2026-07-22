package com.promsearch.auth.interfaces.dto;

import com.promsearch.auth.application.ReissueInfo;

public record ReissueResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {

    public static ReissueResponse from(ReissueInfo info) {
        return new ReissueResponse(
                info.accessToken(),
                info.refreshToken(),
                info.tokenType(),
                info.expiresIn()
        );
    }
}
