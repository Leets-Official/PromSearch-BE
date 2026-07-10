package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User create(User user) {
        try {
            UserJpaEntity savedUser = userJpaRepository.saveAndFlush(UserMapper.toJpaEntity(user));
            return UserMapper.toDomain(savedUser);
        } catch (DataIntegrityViolationException e) {
            if (userJpaRepository.existsByEmail(user.getEmail())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
            }
            if (userJpaRepository.existsByNickname(user.getNickname())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
            }
            throw e;
        }
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
