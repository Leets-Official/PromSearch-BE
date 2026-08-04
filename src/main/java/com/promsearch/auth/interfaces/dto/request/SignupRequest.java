package com.promsearch.auth.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.SignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "서비스에서 사용할 닉네임", example = "prompt-master")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
        String nickname,

        @Schema(description = "로그인 이메일", example = "gildong@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "영문, 숫자, 특수문자 중 2가지 이상을 조합한 로그인 비밀번호", example = "password123!", minLength = 8, maxLength = 20)
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {

    public SignupCommand toCommand() {
        return SignupCommand.of(nickname, email, password);
    }

    /**
     * 요청 DTO가 로깅되더라도 평문 비밀번호는 출력하지 않는다.
     */
    @Override
    public String toString() {
        return "SignupRequest[nickname=" + nickname + ", email=" + email + ", password=***]";
    }
}
