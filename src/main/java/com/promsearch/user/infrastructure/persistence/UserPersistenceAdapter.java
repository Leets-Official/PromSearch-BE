package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        try {
            UserJpaEntity savedUser = userJpaRepository.save(UserMapper.toJpaEntity(user));
            return UserMapper.toDomain(savedUser);
        } catch (DataIntegrityViolationException e) {
            if (userJpaRepository.existsByNickname(user.getNickname())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
            }
            if (userJpaRepository.existsByEmail(user.getEmail())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
            }
            throw e;
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userJpaRepository.existsByNickname(nickname);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
}
