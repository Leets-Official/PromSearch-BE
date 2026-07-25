package com.promsearch.user.interfaces.dto;

import com.promsearch.user.application.PublicUserProfileInfo;
import com.promsearch.user.domain.enums.UserGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "타인 프로필 공개 조회 응답")
public record PublicUserProfileResponse(
        @Schema(description = "사용자 ID", example = "12")
        Long userId,

        @Schema(description = "닉네임", example = "프롬프트장인")
        String nickname,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL", example = "https://cdn.promsearch.com/profiles/12.jpg", nullable = true)
        String profileImageUrl,

        @Schema(description = "사용자 등급", example = "PRIME")
        UserGrade grade,

        @Schema(description = "공개 중인 프롬프트 수", example = "8")
        long promptCount,

        @Schema(description = "공개 중인 프롬프트 누적 좋아요 수", example = "124")
        long totalLikeCount,

        @Schema(description = "공개 중인 프롬프트 누적 조회 수", example = "2300")
        long totalViewCount,

        @Schema(description = "가입 시각", example = "2026-07-23T12:00:00Z")
        Instant createdAt
) {

    public static PublicUserProfileResponse from(PublicUserProfileInfo info) {
        return new PublicUserProfileResponse(
                info.userId(),
                info.nickname(),
                info.name(),
                info.profileImageUrl(),
                info.grade(),
                info.promptCount(),
                info.totalLikeCount(),
                info.totalViewCount(),
                info.createdAt()
        );
    }
}
