package com.promsearch.auth.application;

public interface SocialLoginUseCase {

    LoginInfo socialLogin(SocialLoginCommand command);
}
