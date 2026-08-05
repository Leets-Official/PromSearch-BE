package com.promsearch.user.infrastructure.config;

import com.promsearch.user.application.usecase.BootstrapAdminAccountUseCase;
import com.promsearch.user.application.usecase.dto.BootstrapAdminAccountCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccountBootstrapRunner implements ApplicationRunner {

    private final BootstrapAdminAccountUseCase bootstrapAdminAccountUseCase;
    private final AdminBootstrapProperties adminBootstrapProperties;

    @Override
    public void run(ApplicationArguments args) {
        bootstrapAdminAccountUseCase.bootstrap(BootstrapAdminAccountCommand.of(
                adminBootstrapProperties.email(),
                adminBootstrapProperties.password(),
                adminBootstrapProperties.nickname()
        ));
    }
}
