package com.promsearch.user.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileImageObjectKeyGeneratorTest {

    private final ProfileImageObjectKeyGenerator generator = new ProfileImageObjectKeyGenerator(
            new S3StorageProperties(
                    "bucket",
                    "ap-northeast-2",
                    "prompt-images/original",
                    "prompt-images/watermarked",
                    "/profiles/",
                    "https://cdn.example.com/",
                    Duration.ofMinutes(10)
            )
    );

    @Test
    void generatesProfilesUserIdObjectKey() {
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String objectKey = generator.generate(12L, imageId, ProfileImageContentType.JPEG);

        assertThat(objectKey).isEqualTo("profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg");
        assertThat(generator.isOwnedBy(12L, objectKey)).isTrue();
        assertThat(generator.isOwnedBy(13L, objectKey)).isFalse();
    }

    @Test
    void rejectsNestedOrUnsupportedObjectKeys() {
        assertThat(generator.isOwnedBy(12L, "profiles/12/nested/file.jpg")).isFalse();
        assertThat(generator.isOwnedBy(12L, "profiles/12/123e4567-e89b-12d3-a456-426614174000.webp"))
                .isFalse();
    }
}
