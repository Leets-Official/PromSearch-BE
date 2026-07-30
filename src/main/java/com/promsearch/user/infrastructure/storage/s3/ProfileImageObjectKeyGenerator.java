package com.promsearch.user.infrastructure.storage.s3;

import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import com.promsearch.common.infrastructure.storage.StorageObjectKeyFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 프로필 이미지 접두사와 UUID 파일명 규칙을 적용하는 객체 키 생성 어댑터.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageObjectKeyGenerator implements GenerateProfileImageObjectKeyPort {

    private final ProfileImageStorageProperties properties;
    private final StorageObjectKeyFactory objectKeyFactory;

    /**
     * {@code profile-images/{userId}/{uuid}.{extension}} 형식의 고유 키를 생성한다.
     */
    @Override
    public String generate(Long userId, ProfileImageContentType contentType) {
        return objectKeyFactory.generate(
                properties.profilePrefix(),
                userId,
                UUID.randomUUID(),
                contentType.getExtension()
        );
    }

    /**
     * 사용자 경로에 속하면서 UUID 파일명과 허용 확장자 규칙을 만족하는지 검사한다.
     */
    @Override
    public boolean isOwnedBy(String objectKey, Long userId) {
        if (!objectKeyFactory.isOwnedBy(objectKey, properties.profilePrefix(), userId)) {
            return false;
        }
        String ownerPrefix = properties.normalizedPrefix() + "/" + userId + "/";
        String fileName = objectKey.substring(ownerPrefix.length());
        return fileName.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp)"
        );
    }
}
