package com.promsearch.global.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 프롬프트 이미지와 프로필 이미지 저장소가 공유하는 S3 설정입니다.
 *
 * @param bucket S3 버킷 이름
 * @param region AWS 리전
 * @param originalPrefix 프롬프트 원본 이미지 접두사
 * @param watermarkedPrefix 프롬프트 워터마크 이미지 접두사
 * @param profilePrefix 프로필 이미지 접두사
 * @param profilePublicBaseUrl 프로필 이미지 공개 CDN base URL
 * @param uploadUrlExpiration Presigned 업로드 URL 유효 시간
 */
@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank String originalPrefix,
        @NotBlank String watermarkedPrefix,
        String profilePrefix,
        String profilePublicBaseUrl,
        @NotNull Duration uploadUrlExpiration
) {

    public S3StorageProperties {
        if (uploadUrlExpiration != null
                && (uploadUrlExpiration.isZero() || uploadUrlExpiration.isNegative())) {
            throw new IllegalArgumentException("S3 업로드 URL 만료 시간은 0보다 커야 합니다.");
        }

        originalPrefix = normalizePrefix(originalPrefix);
        watermarkedPrefix = normalizePrefix(watermarkedPrefix);
        profilePrefix = profilePrefix == null || profilePrefix.isBlank()
                ? "profiles"
                : normalizePrefix(profilePrefix);
        if (originalPrefix != null && originalPrefix.equals(watermarkedPrefix)) {
            throw new IllegalArgumentException("S3 원본과 워터마크 결과 접두사는 달라야 합니다.");
        }
        if (profilePrefix.equals(originalPrefix) || profilePrefix.equals(watermarkedPrefix)) {
            throw new IllegalArgumentException("S3 프로필 이미지 접두사는 프롬프트 이미지 접두사와 달라야 합니다.");
        }
        profilePublicBaseUrl = normalizePublicBaseUrl(profilePublicBaseUrl);
    }

    public String resolvedProfilePublicBaseUrl() {
        if (profilePublicBaseUrl != null) {
            return profilePublicBaseUrl;
        }
        return "https://%s.s3.%s.amazonaws.com".formatted(bucket.trim(), region.trim());
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        return prefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private static String normalizePublicBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
