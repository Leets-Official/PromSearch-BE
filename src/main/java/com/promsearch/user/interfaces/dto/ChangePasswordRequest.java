package com.promsearch.user.interfaces.dto;

import com.promsearch.user.application.ChangePasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "oldPassword123!")
        @NotBlank(message = "currentPassword is required")
        @Size(min = 8, max = 100, message = "currentPassword must be between 8 and 100 characters")
        String currentPassword,

        @Schema(description = "영문, 숫자, 특수문자 중 2가지 이상을 조합한 새 비밀번호", example = "newPassword123!", minLength = 8, maxLength = 20)
        @NotBlank(message = "newPassword is required")
        String newPassword
) {

    public ChangePasswordCommand toCommand(Long userId) {
        return ChangePasswordCommand.of(userId, currentPassword, newPassword);
    }

    /**
     * 현재/신규 평문 비밀번호 모두 요청 로깅에서 마스킹한다.
     */
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
