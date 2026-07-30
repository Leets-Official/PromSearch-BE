package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 이미지 업로드 완료 요청")
public record CompleteProfileImageUploadRequest(
        @Schema(description = "업로드 URL 발급 응답의 Object Key")
        @NotBlank @Size(max = 1024) String objectKey,

        @Schema(description = "업로드한 이미지 MIME 타입", example = "image/jpeg")
        @NotBlank String contentType,

        @Schema(description = "업로드한 이미지 파일 크기(byte)", example = "1048576")
        @Min(1) @Max(5_242_880) long fileSize
) {

    /**
     * 인증 사용자와 업로드 결과를 애플리케이션 계층의 완료 명령으로 변환한다.
     */
    public CompleteProfileImageUploadCommand toCommand(Long userId) {
        return CompleteProfileImageUploadCommand.of(userId, objectKey, contentType, fileSize);
    }
}
