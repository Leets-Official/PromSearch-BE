package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "내 프로필 부분 수정 요청")
public record UpdateUserProfileRequest(
        @Schema(description = "변경할 이름. null이면 기존 값을 유지합니다.", example = "홍길동")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 닉네임. null이면 기존 값을 유지합니다.", example = "프롬프트장인")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @Schema(description = "변경할 이메일. null이면 기존 값을 유지합니다.", example = "gildong@example.com")
        String email,

        @Schema(description = "변경할 외부 프로필 이미지 URL. null이면 기존 값을 유지하고 빈 문자열이면 이미지를 제거합니다.", example = "https://cdn.promsearch.com/profiles/me.png")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
        String profileImageUrl,

        @Schema(description = "관심 직군 태그 ID. null이면 기존 값을 유지하고 빈 배열이면 전체 해제합니다.", example = "[1, 2]")
        @Size(max = 3, message = "관심 직군은 최대 3개까지 선택할 수 있습니다.")
        List<Long> interestJobTagIds,

        @Schema(description = "관심 태스크 태그 ID. null이면 기존 값을 유지하고 빈 배열이면 전체 해제합니다.", example = "[10, 11]")
        @Size(max = 3, message = "관심 태스크는 최대 3개까지 선택할 수 있습니다.")
        List<Long> interestTaskTagIds
) {

    public UpdateUserProfileRequest(String name, String nickname, String email) {
        this(name, nickname, email, null, null, null);
    }

    public UpdateUserProfileCommand toCommand(Long userId) {
        return UpdateUserProfileCommand.of(
                userId, name, nickname, email, profileImageUrl, interestJobTagIds, interestTaskTagIds);
    }
}
