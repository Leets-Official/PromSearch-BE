package com.promsearch.user.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 배포 환경에 관리자 계정이 하나도 없을 때 자동으로 만들어 줄 계정 정보다.
 * email/password가 비어 있으면 부트스트랩을 건너뛴다.
 */
@ConfigurationProperties(prefix = "admin.bootstrap")
public record AdminBootstrapProperties(
        String email,
        String password,
        String nickname
) {

    private static final String DEFAULT_NICKNAME = "admin";

    public AdminBootstrapProperties {
        nickname = (nickname == null || nickname.isBlank()) ? DEFAULT_NICKNAME : nickname;
    }
}
