package com.promsearch.user.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.common.infrastructure.storage.StorageObjectKeyFactory;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import org.junit.jupiter.api.Test;

class ProfileImageObjectKeyGeneratorTest {

    private final ProfileImageObjectKeyGenerator generator = new ProfileImageObjectKeyGenerator(
            new ProfileImageStorageProperties("/profile-images/"),
            new StorageObjectKeyFactory()
    );

    @Test
    void generatesUserScopedProfileImageKey() {
        String objectKey = generator.generate(12L, ProfileImageContentType.WEBP);

        assertThat(objectKey).matches(
                "profile-images/12/[0-9a-f-]{36}\\.webp"
        );
        assertThat(generator.isOwnedBy(objectKey, 12L)).isTrue();
        assertThat(generator.isOwnedBy(objectKey, 13L)).isFalse();
    }
}
