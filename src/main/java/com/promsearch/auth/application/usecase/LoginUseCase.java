package com.promsearch.auth.application.usecase;

import com.promsearch.auth.application.usecase.dto.LoginCommand;
import com.promsearch.auth.application.usecase.dto.LoginInfo;

public interface LoginUseCase {

    LoginInfo login(LoginCommand command);
}
