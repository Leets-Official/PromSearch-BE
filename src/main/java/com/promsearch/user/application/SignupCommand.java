package com.promsearch.user.application;

public record SignupCommand(
        String name,
        String nickname,
        String email,
        String password
) {

    public static SignupCommand of(String name, String nickname, String email, String password) {
        return new SignupCommand(name, nickname, email, password);
    }
}
