package com.promsearch.prompt.infrastructure.integration.user;

import com.promsearch.prompt.application.port.out.author.LoadPromptAuthorPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPromptAuthorAdapter implements LoadPromptAuthorPort {

    private final LoadUserPort loadUserPort;

    @Override
    public void validateActive(Long userId) {
        loadUserPort.getById(userId);
    }
}
