package com.promsearch.user.application;

import com.promsearch.auth.domain.CredentialPolicy;

/**
 * 일반 회원가입에 필요한 사용자 정보와 평문 자격증명을 전달한다.
 * <p>
 * HTTP 계층을 거치지 않는 호출에서도 동일한 정책을 보장하기 위해 생성 시점에
 * 이메일과 비밀번호를 인증 도메인의 단일 정책으로 검증한다.
 */
public record SignupCommand(
        String name,
        String nickname,
        String email,
        String password
) {

    public SignupCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(password);
    }

    public static SignupCommand of(String name, String nickname, String email, String password) {
        return new SignupCommand(name, nickname, email, password);
    }

    /**
     * 평문 비밀번호가 로그, 트레이스, 예외 메시지에 우발적으로 노출되지 않도록 마스킹한다.
     */
    @Override
    public String toString() {
        return "SignupCommand[name=" + name + ", nickname=" + nickname + ", email=" + email + ", password=***]";
    }
}
