package com.promsearch.user.application;

import com.promsearch.auth.domain.CredentialPolicy;

/**
 * 현재 비밀번호 확인 후 새 비밀번호로 교체하기 위한 Command.
 */
public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {

    public ChangePasswordCommand {
        // 현재 비밀번호는 기존 자격증명 확인에만 사용하고, 새로 저장할 비밀번호에만 최신 정책을 강제한다.
        CredentialPolicy.validatePassword(newPassword);
    }

    public static ChangePasswordCommand of(Long userId, String currentPassword, String newPassword) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }

    /**
     * 현재/신규 평문 비밀번호 모두 로그, 트레이스, 예외 메시지에서 마스킹한다.
     */
    @Override
    public String toString() {
        return "ChangePasswordCommand[userId=" + userId + ", currentPassword=***, newPassword=***]";
    }
}
