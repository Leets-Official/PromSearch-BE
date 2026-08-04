package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.ProfileImageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로필에 적용된 이미지의 공개 조회 URL 응답입니다.
 *
 * @param profileImageUrl 프로필 이미지 공개 조회 URL
 */
@Schema(description = "적용된 프로필 이미지")
public record ProfileImageResponse(
        @Schema(description = "프로필 이미지 공개 조회 URL")
        String profileImageUrl
) {

    /**
     * @param info 애플리케이션 계층의 프로필 이미지 정보
     * @return HTTP 프로필 이미지 응답
     */
    public static ProfileImageResponse from(ProfileImageInfo info) {
        return new ProfileImageResponse(info.profileImageUrl());
    }
}
