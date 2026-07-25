package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.token.IssueAccessTokenPort;
import com.promsearch.auth.application.port.out.token.IssuedAccessToken;
import com.promsearch.auth.application.usecase.IssueLocalSwaggerTokenUseCase;
import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.LocalSwaggerTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("local")
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalSwaggerTokenService implements IssueLocalSwaggerTokenUseCase {

    private final IssueAccessTokenPort issueAccessTokenPort;

    @Override
    public LocalSwaggerTokenInfo issue(AuthenticatedUserInfo user) {
        IssuedAccessToken token = issueAccessTokenPort.issueAccessToken(user);
        return new LocalSwaggerTokenInfo(token.value(), token.expiresInSeconds(), user);
    }
}
