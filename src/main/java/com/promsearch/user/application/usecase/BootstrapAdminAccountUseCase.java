package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.BootstrapAdminAccountCommand;

public interface BootstrapAdminAccountUseCase {

    void bootstrap(BootstrapAdminAccountCommand command);
}
