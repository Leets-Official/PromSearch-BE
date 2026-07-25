package com.promsearch.auth.application.usecase;

import com.promsearch.auth.application.usecase.dto.LoginInfo;
import com.promsearch.auth.application.usecase.dto.SocialLoginCommand;

public interface SocialLoginUseCase {

    LoginInfo socialLogin(SocialLoginCommand command);
}
