package com.promsearch.auth.application.usecase;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.dto.LocalSwaggerTokenInfo;

public interface IssueLocalSwaggerTokenUseCase {

    LocalSwaggerTokenInfo issue(AuthenticatedUserInfo user);
}
