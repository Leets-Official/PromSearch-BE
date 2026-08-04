package com.promsearch.user.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 프로필 이미지 객체를 다른 이미지 용도와 분리하는 S3 경로 설정.
 *
 * @param profilePrefix 프로필 이미지 객체 키의 최상위 접두사
 */
@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record ProfileImageStorageProperties(
        @NotBlank String profilePrefix
) {

    /**
     * 설정값 앞뒤의 슬래시를 제거하여 객체 키 조합에 사용할 접두사를 반환한다.
     */
    public String normalizedPrefix() {
        return profilePrefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
