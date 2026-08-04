package com.promsearch.admin.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.OriginUserSummaryInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Origin 등급 유저 목록 응답")
public record OriginUserSummaryResponse(
        @Schema(description = "유저 식별자", example = "5")
        Long userId,
        @Schema(description = "닉네임", example = "hanharam")
        String username
) {

    public static OriginUserSummaryResponse from(OriginUserSummaryInfo info) {
        return new OriginUserSummaryResponse(info.userId(), info.username());
    }
}
