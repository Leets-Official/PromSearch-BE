package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.ChangePasswordCommand;

public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);
}
