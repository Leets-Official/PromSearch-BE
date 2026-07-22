package com.promsearch.auth.application;

import com.promsearch.auth.domain.CredentialPolicy;

/**
 * 이메일 기반 로그인 자격증명을 전달하는 Command.
 */
public record LoginCommand(
        String email,
        String password
) {

    public LoginCommand {
        CredentialPolicy.validateEmail(email);
        // 기존 비밀번호 확인 단계이므로 가입 이후 강화된 복잡도 정책은 다시 적용하지 않는다.
    }

    public static LoginCommand of(String email, String password) {
        return new LoginCommand(email, password);
    }
}
