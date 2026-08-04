package com.promsearch.user.domain.enums;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Arrays;
import java.util.Locale;

/**
 * 프로필 이미지 업로드에서 허용하는 이미지 형식과 저장 확장자를 정의합니다.
 */
public enum ProfileImageContentType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png");

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
     * 요청 MIME 타입을 지원되는 프로필 이미지 형식으로 변환합니다.
     *
     * @param mimeType 변환할 MIME 타입
     * @return 지원되는 프로필 이미지 형식
     * @throws UserDomainException MIME 타입이 비어 있거나 지원되지 않는 경우
     */
    public static ProfileImageContentType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new UserDomainException(UserErrorCode.UNSUPPORTED_PROFILE_IMAGE_CONTENT_TYPE);
        }

        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.mimeType.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new UserDomainException(
                        UserErrorCode.UNSUPPORTED_PROFILE_IMAGE_CONTENT_TYPE
                ));
    }
}
