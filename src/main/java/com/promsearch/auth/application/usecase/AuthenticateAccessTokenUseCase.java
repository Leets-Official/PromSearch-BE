package com.promsearch.auth.application.usecase;

import com.promsearch.auth.application.usecase.dto.AuthenticatedAccessTokenInfo;

public interface AuthenticateAccessTokenUseCase {

    AuthenticatedAccessTokenInfo authenticate(String accessToken);
}
