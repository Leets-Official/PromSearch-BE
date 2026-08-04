package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 프로필 이미지 S3 직접 업로드 URL 발급 요청 DTO입니다.
 *
 * @param contentType 업로드 이미지 MIME 타입
 * @param fileSize 업로드 파일 크기(byte), 최대 5MB
 */
@Schema(description = "프로필 이미지 S3 직접 업로드 URL 발급 요청")
public record ProfileImageUploadUrlRequest(
        @Schema(
                description = "이미지 MIME 타입. 발급된 URL로 PUT할 때 같은 Content-Type 헤더를 보내야 합니다.",
                example = "image/jpeg",
                allowableValues = {"image/jpeg", "image/png"}
        )
        @NotBlank(message = "이미지 MIME 타입은 필수입니다.")
        @Pattern(regexp = "image/(jpeg|png)", message = "프로필 이미지는 JPEG 또는 PNG만 지원합니다.")
        String contentType,

        @Schema(description = "이미지 파일 크기(byte). 최대 5MB입니다.", example = "1048576")
        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        @Max(value = 5_242_880, message = "프로필 이미지는 5MB 이하여야 합니다.")
        Long fileSize
) {

    /**
     * 인증 사용자 ID를 결합해 애플리케이션 명령으로 변환합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 프로필 이미지 업로드 URL 발급 명령
     */
    public IssueProfileImageUploadUrlCommand toCommand(Long userId) {
        return new IssueProfileImageUploadUrlCommand(userId, contentType, fileSize);
    }
}
