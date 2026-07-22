package com.promsearch.user.application;

import com.promsearch.auth.domain.CredentialPolicy;

/**
 * 검증된 OAuth 사용자 정보로 신규 사용자를 생성하기 위한 Command.
 */
public record RegisterSocialUserCommand(
        String email,
        String nickname,
        String name,
        String profileImageUrl
) {

    public RegisterSocialUserCommand {
        // 외부 제공자 응답도 신뢰 경계 밖의 입력이므로 저장 전에 서비스 이메일 정책을 적용한다.
        CredentialPolicy.validateEmail(email);
    }

    public static RegisterSocialUserCommand of(String email, String nickname, String name, String profileImageUrl) {
        return new RegisterSocialUserCommand(email, nickname, name, profileImageUrl);
    }
}
