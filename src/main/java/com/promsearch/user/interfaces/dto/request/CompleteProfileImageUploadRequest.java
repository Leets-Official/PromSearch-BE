package com.promsearch.user.interfaces.dto.request;

import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 업로드 URL로 S3 전송을 마친 프로필 이미지를 적용하는 요청입니다.
 *
 * @param objectKey 업로드 URL 발급 응답으로 받은 Object Key
 */
@Schema(description = "프로필 이미지 업로드 완료 요청")
public record CompleteProfileImageUploadRequest(
        @Schema(description = "업로드 URL 발급 응답의 Object Key")
        @NotBlank(message = "Object Key는 필수입니다.")
        @Size(max = 1024, message = "Object Key는 1024자 이하여야 합니다.")
        String objectKey
) {

    /**
     * 인증 사용자와 업로드 결과를 애플리케이션 계층의 완료 명령으로 변환한다.
     */
    public CompleteProfileImageUploadCommand toCommand(Long userId) {
        return new CompleteProfileImageUploadCommand(userId, objectKey);
    }
}
