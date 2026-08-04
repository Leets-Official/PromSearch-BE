package com.promsearch.user.infrastructure.storage.s3;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code profiles/{userId}/{uuid}.{extension}} 규칙으로 프로필 이미지 Object Key를 관리합니다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageObjectKeyGenerator implements GenerateProfileImageObjectKeyPort {

    private final S3StorageProperties properties;

    @Override
    public String generate(Long userId, UUID imageId, ProfileImageContentType contentType) {
        return "%s/%d/%s.%s".formatted(
                properties.profilePrefix(),
                userId,
                imageId,
                contentType.getExtension()
        );
    }

    @Override
    public boolean isOwnedBy(Long userId, String objectKey) {
        if (userId == null || userId <= 0 || objectKey == null || objectKey.isBlank()) {
            return false;
        }

        String prefix = "%s/%d/".formatted(properties.profilePrefix(), userId);
        if (!objectKey.startsWith(prefix)) {
            return false;
        }

        String fileName = objectKey.substring(prefix.length());
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator <= 0 || extensionSeparator == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(extensionSeparator + 1);
        if (!extension.equals("jpg") && !extension.equals("png")) {
            return false;
        }

        try {
            UUID.fromString(fileName.substring(0, extensionSeparator));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
