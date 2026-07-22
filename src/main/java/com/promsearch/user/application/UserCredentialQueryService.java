package com.promsearch.user.application;

import com.promsearch.user.application.port.out.UserCredentialReader;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCredentialQueryService implements GetUserCredentialUseCase {

    private final UserCredentialReader userCredentialReader;

    @Override
    public Optional<AuthUserInfo> findByEmail(String email) {
        return userCredentialReader.findByEmail(email);
    }

    @Override
    public Optional<AuthUserInfo> findById(Long userId) {
        return userCredentialReader.findById(userId);
    }
}
