package com.promsearch.prompt.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank String originalPrefix,
        @NotNull Duration uploadUrlExpiration
) {

    public S3StorageProperties {
        if (uploadUrlExpiration != null
                && (uploadUrlExpiration.isZero() || uploadUrlExpiration.isNegative())) {
            throw new IllegalArgumentException("S3 업로드 URL 만료 시간은 0보다 커야 합니다.");
        }
    }
}
