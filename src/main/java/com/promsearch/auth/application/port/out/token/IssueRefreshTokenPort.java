package com.promsearch.auth.application.port.out.token;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;

public interface IssueRefreshTokenPort {

    IssuedRefreshToken issueRefreshToken(AuthenticatedUserInfo user);
}
