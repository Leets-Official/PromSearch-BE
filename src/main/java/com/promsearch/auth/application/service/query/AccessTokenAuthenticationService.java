package com.promsearch.auth.application.service.query;

import com.promsearch.auth.application.port.out.token.AccessTokenClaims;
import com.promsearch.auth.application.port.out.token.VerifyAccessTokenPort;
import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.auth.application.usecase.dto.AuthenticatedAccessTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessTokenAuthenticationService implements AuthenticateAccessTokenUseCase {

    private final VerifyAccessTokenPort verifyAccessTokenPort;

    @Override
    public AuthenticatedAccessTokenInfo authenticate(String accessToken) {
        AccessTokenClaims claims = verifyAccessTokenPort.verifyAccessToken(accessToken);
        return new AuthenticatedAccessTokenInfo(claims.userId(), claims.role());
    }
}
