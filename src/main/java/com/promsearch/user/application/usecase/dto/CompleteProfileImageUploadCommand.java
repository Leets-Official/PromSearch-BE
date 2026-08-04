package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;

/**
 * 프로필 이미지 업로드 완료 및 교체 명령입니다.
 *
 * @param userId 이미지를 적용할 사용자 ID
 * @param objectKey 업로드 URL 발급 단계에서 받은 Object Key
 */
public record CompleteProfileImageUploadCommand(Long userId, String objectKey) {

    public CompleteProfileImageUploadCommand {
        if (userId == null || userId <= 0) {
            throw new UserDomainException(UserErrorCode.INVALID_ID);
        }
        if (objectKey == null || objectKey.isBlank() || objectKey.length() > 500) {
            throw new UserDomainException(UserErrorCode.INVALID_PROFILE_IMAGE_OBJECT_KEY);
        }
        objectKey = objectKey.trim();
    }
}
