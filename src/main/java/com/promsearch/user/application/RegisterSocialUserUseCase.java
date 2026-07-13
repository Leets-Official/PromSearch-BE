package com.promsearch.user.application;

public interface RegisterSocialUserUseCase {

    SignupInfo registerSocialUser(RegisterSocialUserCommand command);
}
