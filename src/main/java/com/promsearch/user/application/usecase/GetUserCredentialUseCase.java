package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.AuthUserInfo;
import java.util.Optional;

public interface GetUserCredentialUseCase {

    Optional<AuthUserInfo> findByEmail(String email);

    Optional<AuthUserInfo> findById(Long userId);
}
