package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.RegisterSocialUserCommand;
import com.promsearch.user.application.usecase.dto.SignupInfo;

public interface RegisterSocialUserUseCase {

    SignupInfo registerSocialUser(RegisterSocialUserCommand command);
}
