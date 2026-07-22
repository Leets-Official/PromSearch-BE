package com.promsearch.auth.application;

public interface LoginUseCase {

    LoginInfo login(LoginCommand command);
}
