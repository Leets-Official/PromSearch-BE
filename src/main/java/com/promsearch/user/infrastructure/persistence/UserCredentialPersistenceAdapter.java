package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.AuthUserInfo;
import com.promsearch.user.application.port.out.UserCredentialReader;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserCredentialPersistenceAdapter implements UserCredentialReader {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<AuthUserInfo> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(UserMapper::toDomain)
                .map(AuthUserInfo::from);
    }

    @Override
    public Optional<AuthUserInfo> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(UserMapper::toDomain)
                .map(AuthUserInfo::from);
    }
}
