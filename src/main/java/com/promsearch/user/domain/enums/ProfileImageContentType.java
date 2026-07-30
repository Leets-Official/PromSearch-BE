package com.promsearch.user.domain.enums;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Locale;

/**
 * 서비스가 프로필 이미지 업로드로 허용하는 MIME 타입과 저장 확장자의 대응 관계.
 */
public enum ProfileImageContentType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    ProfileImageContentType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * 요청 MIME 타입을 정규화하여 허용된 이미지 타입으로 변환한다.
     *
     * @param mimeType 클라이언트가 전달한 MIME 타입
     * @return MIME 타입에 대응하는 프로필 이미지 타입
     * @throws UserDomainException 지원하지 않거나 비어 있는 MIME 타입인 경우
     */
    public static ProfileImageContentType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_PROFILE_IMAGE_CONTENT_TYPE);
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        for (ProfileImageContentType contentType : values()) {
            if (contentType.mimeType.equals(normalized)) {
                return contentType;
            }
        }
        throw new UserDomainException(UserErrorCode.INVALID_PROFILE_IMAGE_CONTENT_TYPE);
    }
}
