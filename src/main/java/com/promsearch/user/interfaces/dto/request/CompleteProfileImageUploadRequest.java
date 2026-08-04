package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * S3 업로드 완료 후 프로필 이미지 적용을 요청하는 HTTP 요청 DTO입니다.
 *
 * @param objectKey 업로드 URL 발급 응답으로 받은 Object Key
 */
@Schema(description = "프로필 이미지 업로드 완료 및 교체 요청")
public record CompleteProfileImageUploadRequest(
        @Schema(
                description = "업로드 URL 발급 응답으로 받은 Object Key",
                example = "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg"
        )
        @NotBlank(message = "Object Key는 필수입니다.")
        @Size(max = 500, message = "Object Key는 500자 이하여야 합니다.")
        String objectKey
) {

    /**
     * 인증 사용자 ID를 결합해 애플리케이션 명령으로 변환합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 프로필 이미지 업로드 완료 명령
     */
    public CompleteProfileImageUploadCommand toCommand(Long userId) {
        return new CompleteProfileImageUploadCommand(userId, objectKey);
    }
}
