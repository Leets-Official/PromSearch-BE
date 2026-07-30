package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로필 이미지 업로드 URL 발급 요청")
public record ProfileImageUploadUrlRequest(
        @Schema(description = "이미지 MIME 타입", example = "image/jpeg")
        @NotBlank String contentType,

        @Schema(description = "이미지 파일 크기(byte)", example = "1048576")
        @Min(1) @Max(5_242_880) long fileSize
) {

    /**
     * 인증 정보에서 얻은 사용자 식별자를 요청 본문과 결합해 애플리케이션 명령으로 변환한다.
     */
    public IssueProfileImageUploadUrlCommand toCommand(Long userId) {
        return IssueProfileImageUploadUrlCommand.of(userId, contentType, fileSize);
    }
}
