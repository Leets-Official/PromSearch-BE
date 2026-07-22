package com.promsearch.user.application.port.out;

import com.promsearch.user.application.AuthUserInfo;
import java.util.Optional;

public interface UserCredentialReader {

    Optional<AuthUserInfo> findByEmail(String email);

    Optional<AuthUserInfo> findById(Long userId);
}
