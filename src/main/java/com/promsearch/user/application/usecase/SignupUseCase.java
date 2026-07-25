package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.SignupCommand;
import com.promsearch.user.application.usecase.dto.SignupInfo;

public interface SignupUseCase {

    SignupInfo signup(SignupCommand command);
}
