package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        String email
) {

    public UpdateUserProfileCommand toCommand(Long userId) {
        return UpdateUserProfileCommand.of(userId, name, nickname, email);
    }
}
