package com.promsearch.common.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 프롬프트와 프로필 이미지가 함께 사용하는 S3 연결 및 서명 URL 설정.
 *
 * @param bucket 객체를 저장할 S3 버킷명
 * @param region 버킷이 위치한 AWS 리전
 * @param uploadUrlExpiration Presigned PUT/GET URL 유효 시간
 */
@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3ObjectStorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotNull Duration uploadUrlExpiration
) {

    /**
     * 애플리케이션 시작 시 잘못된 URL 만료 설정을 조기에 차단한다.
     */
    public S3ObjectStorageProperties {
        if (uploadUrlExpiration != null
                && (uploadUrlExpiration.isZero() || uploadUrlExpiration.isNegative())) {
            throw new IllegalArgumentException("S3 URL 만료 시간은 0보다 커야 합니다.");
        }
    }
}
