package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;

@Schema(description = "프로필 이미지 Presigned PUT URL")
public record ProfileImageUploadUrlResponse(
        String objectKey,
        URI uploadUrl,
        Instant expiresAt
) {

    /**
     * 애플리케이션 결과를 외부 API 응답 모델로 변환한다.
     */
    public static ProfileImageUploadUrlResponse from(ProfileImageUploadUrlInfo info) {
        return new ProfileImageUploadUrlResponse(info.objectKey(), info.uploadUrl(), info.expiresAt());
    }
}
