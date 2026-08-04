package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;

/**
 * 직접 업로드된 프로필 이미지를 사용자에게 연결하기 위한 애플리케이션 명령.
 *
 * @param userId 인증된 사용자 식별자
 * @param objectKey URL 발급 단계에서 서버가 생성한 객체 키
 */
public record CompleteProfileImageUploadCommand(
        Long userId,
        String objectKey
) {

    public CompleteProfileImageUploadCommand {
        if (userId == null || userId <= 0) {
            throw new UserDomainException(UserErrorCode.INVALID_ID);
        }
        if (objectKey == null || objectKey.isBlank() || objectKey.length() > 1024) {
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);
        }
        objectKey = objectKey.trim();
    }
}
