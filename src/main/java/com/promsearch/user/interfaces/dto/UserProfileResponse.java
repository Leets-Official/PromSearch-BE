package com.promsearch.user.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 프로필 조회 응답")
public record UserProfileResponse(
        @Schema(description = "닉네임", example = "hanharam")
        String username,
        @Schema(description = "프로필 이미지 URL", example = "https://example-bucket.s3.amazonaws.com/profile/1.png")
        String profileImageUrl,
        @Schema(description = "이메일", example = "user@promsearch.com")
        String email,
        @Schema(description = "보유 포인트", example = "1200")
        Long point,
        @Schema(description = "크리에이터 등급 이름", example = "ORIGIN")
        String gradeName
) {
}
