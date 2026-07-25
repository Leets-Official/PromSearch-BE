package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;

/**
 * 사용자 프로필 부분 변경 Command. null 필드는 기존 값을 유지한다.
 */
public record UpdateUserProfileCommand(
        Long userId,
        String name,
        String nickname,
        String email,
        String profileImageUrl
) {

    public UpdateUserProfileCommand {
        // 프로필 서비스가 앞뒤 공백을 제거하므로 실제 저장될 값을 기준으로 정책을 적용한다.
        if (email != null && !email.isBlank()) {
            CredentialPolicy.validateEmail(email.trim());
        }
    }

    public static UpdateUserProfileCommand of(
            Long userId,
            String name,
            String nickname,
            String email,
            String profileImageUrl
    ) {
        return new UpdateUserProfileCommand(userId, name, nickname, email, profileImageUrl);
    }
}
