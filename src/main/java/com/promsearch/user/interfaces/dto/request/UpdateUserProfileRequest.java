package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "내 프로필 부분 수정 요청")
public record UpdateUserProfileRequest(
        @Schema(description = "변경할 닉네임. null이면 기존 값을 유지합니다.", example = "프롬프트장인")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @Schema(description = "변경할 이메일. null이면 기존 값을 유지합니다.", example = "gildong@example.com")
        String email,

        @Schema(description = "변경할 관심 직군 태그 ID(최대 3개). null이면 기존 값을 유지하며, "
                + "관심 태그를 변경하려면 taskTagIds와 함께 보내야 합니다.", example = "[1, 2]")
        @Size(max = 3, message = "관심 직군은 최대 3개까지 선택할 수 있습니다.")
        List<Long> jobTagIds,

        @Schema(description = "변경할 관심 태스크 태그 ID(최대 3개). null이면 기존 값을 유지하며, "
                + "관심 태그를 변경하려면 jobTagIds와 함께 보내야 합니다.", example = "[10, 11]")
        @Size(max = 3, message = "관심 태스크는 최대 3개까지 선택할 수 있습니다.")
        List<Long> taskTagIds
) {

    public UpdateUserProfileCommand toCommand(Long userId) {
        return UpdateUserProfileCommand.of(userId, nickname, email, jobTagIds, taskTagIds);
    }
}
