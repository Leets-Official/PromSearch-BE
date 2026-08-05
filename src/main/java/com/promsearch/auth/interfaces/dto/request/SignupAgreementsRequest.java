package com.promsearch.auth.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.SignupAgreements;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "회원가입 약관 동의")
public record SignupAgreementsRequest(
        @Schema(description = "프롬서치 이용약관 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "프롬서치 이용약관 동의 여부는 필수입니다.")
        @AssertTrue(message = "프롬서치 이용약관에 동의해야 합니다.")
        Boolean serviceTerms,
        @Schema(description = "커뮤니티 이용규칙 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "커뮤니티 이용규칙 동의 여부는 필수입니다.")
        @AssertTrue(message = "커뮤니티 이용규칙에 동의해야 합니다.")
        Boolean communityTerms,
        @Schema(description = "콘텐츠 업로드 및 저작권 정책 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "콘텐츠 업로드 및 저작권 정책 동의 여부는 필수입니다.")
        @AssertTrue(message = "콘텐츠 업로드 및 저작권 정책에 동의해야 합니다.")
        Boolean contentPolicy,
        @Schema(description = "만 14세 이상 확인", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "만 14세 이상 확인은 필수입니다.")
        @AssertTrue(message = "만 14세 이상이어야 가입할 수 있습니다.")
        Boolean age14OrOver,
        @Schema(description = "마케팅 정보 수신 동의(선택)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "마케팅 정보 수신 동의 여부는 필수입니다.")
        Boolean marketing
) {
    public SignupAgreements toCommand() {
        return SignupAgreements.of(serviceTerms, communityTerms, contentPolicy, age14OrOver, marketing);
    }

    public static SignupAgreementsRequest requiredAndNoMarketing() {
        return new SignupAgreementsRequest(true, true, true, true, false);
    }
}
