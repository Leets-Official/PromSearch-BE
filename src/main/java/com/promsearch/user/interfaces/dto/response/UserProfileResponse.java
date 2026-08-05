package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.UserProfileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

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
        String gradeName,
        @Schema(description = "관심 직군 태그")
        List<InterestTagResponse> interestJobTags,
        @Schema(description = "관심 태스크 태그")
        List<InterestTagResponse> interestTaskTags
) {

    public static UserProfileResponse from(UserProfileInfo info) {
        return new UserProfileResponse(
                info.username(),
                info.profileImageUrl(),
                info.email(),
                info.point(),
                info.gradeName(),
                info.jobTags().stream().map(InterestTagResponse::from).toList(),
                info.taskTags().stream().map(InterestTagResponse::from).toList()
        );
    }

    public record InterestTagResponse(
            @Schema(description = "태그 ID", example = "1") Long tagId,
            @Schema(description = "태그 이름", example = "직장인") String name
    ) {
        private static InterestTagResponse from(com.promsearch.user.application.usecase.dto.InterestTagInfo info) {
            return new InterestTagResponse(info.tagId(), info.tagName());
        }
    }
}
