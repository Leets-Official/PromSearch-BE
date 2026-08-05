package com.promsearch.user.application.usecase.dto;

/**
 * 배포 환경 기동 시 관리자 계정이 없으면 생성하기 위한 Command.
 */
public record BootstrapAdminAccountCommand(
        String email,
        String password,
        String nickname
) {

    public static BootstrapAdminAccountCommand of(String email, String password, String nickname) {
        return new BootstrapAdminAccountCommand(email, password, nickname);
    }

    @Override
    public String toString() {
        return "BootstrapAdminAccountCommand[email=" + email + ", password=***, nickname=" + nickname + "]";
    }
}
