package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
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
    public User getById(Long userId) {
        return UserMapper.toDomain(getActiveUserJpaEntity(userId));
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userJpaRepository.existsByNickname(nickname);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User updateProfile(
            Long userId,
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl
    ) {
        UserJpaEntity user = getActiveUserJpaEntity(userId);
        user.updateProfile(email, password, nickname, name, profileImageUrl);
        return UserMapper.toDomain(user);
    }

    @Override
    public void deleteById(Long userId) {
        UserJpaEntity user = getActiveUserJpaEntity(userId);
        user.delete();
    }

    private UserJpaEntity getActiveUserJpaEntity(Long userId) {
        return userJpaRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));
    }
}
