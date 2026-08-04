package com.promsearch.user.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProfileImageStorageConfigurationValidatorTest {

    @Test
    void prodRequiresPublicBaseUrl() {
        ProfileImageStorageConfigurationValidator validator = new ProfileImageStorageConfigurationValidator(
                properties(null),
                new MockEnvironment().withProperty("spring.profiles.active", "prod")
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS_S3_PROFILE_PUBLIC_BASE_URL");
    }

    @Test
    void localMayUseStandardS3UrlFallback() {
        S3StorageProperties properties = properties(null);
        ProfileImageStorageConfigurationValidator validator = new ProfileImageStorageConfigurationValidator(
                properties,
                new MockEnvironment().withProperty("spring.profiles.active", "local")
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
        assertThat(properties.resolvedProfilePublicBaseUrl())
                .isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com");
    }

    private S3StorageProperties properties(String profilePublicBaseUrl) {
        return new S3StorageProperties(
                "test-bucket",
                "ap-northeast-2",
                "prompt-images/original",
                "prompt-images/watermarked",
                "profiles",
                profilePublicBaseUrl,
                Duration.ofMinutes(10)
        );
    }
}
