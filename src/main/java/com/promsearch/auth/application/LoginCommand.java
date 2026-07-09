package com.promsearch.auth.application;

public record LoginCommand(
        String email,
        String password
) {

    public static LoginCommand of(String email, String password) {
        return new LoginCommand(email, password);
    }
}
