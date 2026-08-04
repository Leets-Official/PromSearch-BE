package com.promsearch.prompt.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        @NotBlank String originalPrefix,
        @NotBlank String watermarkedPrefix
) {

    public S3StorageProperties {
        if (originalPrefix != null
                && watermarkedPrefix != null
                && normalizePrefix(originalPrefix).equals(normalizePrefix(watermarkedPrefix))) {
            throw new IllegalArgumentException("S3 원본과 워터마크 결과 접두사는 달라야 합니다.");
        }
    }

    /** 원본·결과 접두사 비교를 위해 앞뒤 슬래시를 제거 */
    private static String normalizePrefix(String prefix) {
        return prefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
