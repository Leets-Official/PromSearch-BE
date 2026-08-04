package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 사용 가능 여부")
public record NicknameAvailabilityResponse(
        @Schema(description = "사용 가능한 닉네임이면 true", example = "true")
        boolean available
) {

    public static NicknameAvailabilityResponse from(NicknameAvailabilityInfo info) {
        return new NicknameAvailabilityResponse(info.available());
    }
}
