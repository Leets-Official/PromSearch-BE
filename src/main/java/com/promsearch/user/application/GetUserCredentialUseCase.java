package com.promsearch.user.application;

import java.util.Optional;

public interface GetUserCredentialUseCase {

    Optional<AuthUserInfo> findByEmail(String email);

    Optional<AuthUserInfo> findById(Long userId);
}
