package com.promsearch.auth.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.SignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "한글, 영문, 숫자로 구성된 닉네임", example = "프롬프트장인")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,
        @Schema(description = "로그인 이메일", example = "gildong@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,
        @Schema(description = "로그인 비밀번호", example = "password123!", minLength = 8, maxLength = 20)
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        @Schema(description = "관심 직군 태그 ID(최대 3개)", example = "[1, 2]")
        @Size(max = 3, message = "관심 직군은 최대 3개까지 선택할 수 있습니다.")
        List<Long> interestJobTagIds,
        @Schema(description = "관심 태스크 태그 ID(최대 3개)", example = "[10, 11]")
        @Size(max = 3, message = "관심 태스크는 최대 3개까지 선택할 수 있습니다.")
        List<Long> interestTaskTagIds,
        @Schema(description = "약관 동의", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotNull(message = "약관 동의는 필수입니다.")
        SignupAgreementsRequest agreements
) {
    public SignupRequest(String nickname, String email, String password) {
        this(nickname, email, password, List.of(), List.of(), SignupAgreementsRequest.requiredAndNoMarketing());
    }

    public SignupRequest(String nickname, String email, String password,
                         List<Long> interestJobTagIds, List<Long> interestTaskTagIds) {
        this(nickname, email, password, interestJobTagIds, interestTaskTagIds,
                SignupAgreementsRequest.requiredAndNoMarketing());
    }

    public SignupCommand toCommand() {
        return SignupCommand.of(nickname, email, password, interestJobTagIds, interestTaskTagIds,
                agreements.toCommand());
    }

    @Override
    public String toString() {
        return "SignupRequest[nickname=" + nickname + ", email=" + email
                + ", password=***, interestJobTagIds=" + interestJobTagIds
                + ", interestTaskTagIds=" + interestTaskTagIds + ", agreements=" + agreements + "]";
    }
}
