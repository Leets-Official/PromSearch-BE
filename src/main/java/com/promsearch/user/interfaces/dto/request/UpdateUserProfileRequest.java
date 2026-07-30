package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "내 프로필 부분 수정 요청")
public record UpdateUserProfileRequest(
        @Schema(description = "변경할 이름. null이면 기존 값을 유지합니다.", example = "홍길동")
        @Size(max = 100, message = "name must be 100 characters or less")
        String name,

        @Schema(description = "변경할 닉네임. null이면 기존 값을 유지합니다.", example = "prompt-master")
        @Size(max = 100, message = "nickname must be 100 characters or less")
        String nickname,

        @Schema(description = "변경할 이메일. null이면 기존 값을 유지합니다.", example = "gildong@example.com")
        String email
) {

    public UpdateUserProfileCommand toCommand(Long userId) {
        return UpdateUserProfileCommand.of(userId, name, nickname, email);
    }
}
