package com.promsearch.auth.interfaces.dto.request;

import com.promsearch.auth.application.usecase.dto.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 이메일", example = "gildong@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "로그인 비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
        String password
) {

    public LoginCommand toCommand() {
        return LoginCommand.of(email, password);
    }

    /**
     * 요청 DTO가 로깅되더라도 평문 비밀번호는 출력하지 않는다.
     */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
