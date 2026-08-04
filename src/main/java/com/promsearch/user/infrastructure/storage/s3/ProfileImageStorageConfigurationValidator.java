package com.promsearch.user.infrastructure.storage.s3;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProfileImageStorageConfigurationValidator {

    private final S3StorageProperties properties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && properties.profilePublicBaseUrl() == null) {
            throw new IllegalStateException(
                    "prod 환경에서는 AWS_S3_PROFILE_PUBLIC_BASE_URL 설정이 필요합니다."
            );
        }
    }
}
